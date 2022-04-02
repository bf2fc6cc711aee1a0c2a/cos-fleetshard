package org.bf2.cos.fleetshard.sync.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.support.watch.Informers;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;

@ApplicationScoped
public class FleetShardClient {
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    private volatile SharedIndexInformer<ManagedConnector> connectorsInformer;
    private volatile SharedIndexInformer<ManagedConnectorOperator> operatorsInformer;
    private volatile SharedIndexInformer<Namespace> namespaceInformers;

    public void start() {
        operatorsInformer = kubernetesClient.resources(ManagedConnectorOperator.class)
            .inNamespace(config.operators().namespace())
            .inform();
        namespaceInformers = kubernetesClient.namespaces()
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .inform();
        connectorsInformer = kubernetesClient.resources(ManagedConnector.class)
            .inAnyNamespace()
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .inform();
    }

    public void stop() {
        Resources.closeQuietly(operatorsInformer);
        Resources.closeQuietly(namespaceInformers);
        Resources.closeQuietly(connectorsInformer);
    }

    public String getClusterId() {
        return config.cluster().id();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public long getMaxDeploymentResourceRevision() {
        return this.connectorsInformer.getIndexer().list().stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);
    }

    public long getMaxNamespaceResourceRevision() {
        return this.namespaceInformers.getIndexer().list().stream()
            .mapToLong(c -> {
                String rv = Resources.getAnnotation(c, Resources.ANNOTATION_NAMESPACE_RESOURCE_VERSION);
                if (rv == null) {
                    return 0;
                }

                return Long.parseLong(rv);
            })
            .max()
            .orElse(0);
    }

    // *************************************
    //
    // Namespaces
    //
    // *************************************

    public Optional<Namespace> getNamespace(String namespaceId) {
        return Optional.ofNullable(
            this.kubernetesClient
                .namespaces()
                .withName(generateNamespaceId(namespaceId))
                .get());
    }

    public List<Namespace> getNamespaces() {
        return namespaceInformers != null
            ? namespaceInformers.getIndexer().list()
            : Collections.emptyList();
    }

    public String generateNamespaceId(String namespaceId) {
        if (!config.tenancy().enabled()) {
            return namespaceId;
        }

        String prefix = config.tenancy().namespacePrefix();
        if (!prefix.endsWith("-")) {
            prefix = prefix + "-";
        }

        return namespaceId.startsWith(prefix)
            ? namespaceId
            : prefix + namespaceId;
    }

    // *************************************
    //
    // Secrets
    //
    // *************************************

    public Secret createSecret(Secret secret) {
        return this.kubernetesClient.secrets()
            .inNamespace(secret.getMetadata().getNamespace())
            .withName(secret.getMetadata().getName())
            .createOrReplace(secret);
    }

    public Optional<Secret> getSecret(ConnectorDeployment deployment) {
        return getSecret(
            deployment.getSpec().getNamespaceId(),
            deployment.getId());
    }

    public Optional<Secret> getSecret(NamespacedName id) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(id.getNamespace())
                .withName(id.getName())
                .get());
    }

    public Optional<Secret> getSecret(String namespaceId, String deploymentId) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(generateNamespaceId(namespaceId))
                .withName(Secrets.generateConnectorSecretId(deploymentId))
                .get());
    }

    // *************************************
    //
    // Connectors
    //
    // *************************************

    public Boolean deleteConnector(ManagedConnector managedConnector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(managedConnector.getMetadata().getNamespace())
            .withName(managedConnector.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public Optional<ManagedConnector> getConnector(NamespacedName id) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        final String key = Cache.namespaceKeyFunc(id.getNamespace(), id.getName());
        final ManagedConnector val = connectorsInformer.getIndexer().getByKey(key);

        return Optional.ofNullable(val);
    }

    public Optional<ManagedConnector> getConnector(ConnectorDeployment deployment) {
        return getConnector(
            deployment.getSpec().getNamespaceId(),
            deployment.getId());
    }

    public Optional<ManagedConnector> getConnector(String namespaceId, String deploymentId) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        final String key = Cache.namespaceKeyFunc(generateNamespaceId(namespaceId), generateConnectorId(deploymentId));
        final ManagedConnector val = connectorsInformer.getIndexer().getByKey(key);

        return Optional.ofNullable(val);
    }

    public List<ManagedConnector> getAllConnectors() {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        return connectorsInformer.getIndexer().list();
    }

    public void watchConnectors(Consumer<ManagedConnector> handler) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        connectorsInformer.addEventHandler(Informers.wrap(handler));
    }

    public void watchConnectors(ResourceEventHandler<ManagedConnector> handler) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        connectorsInformer.addEventHandler(handler);
    }

    public ManagedConnector createConnector(ManagedConnector connector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .createOrReplace(connector);
    }

    public String generateConnectorId(String namespaceId) {
        return Connectors.generateConnectorId(namespaceId);
    }

    // *************************************
    //
    // Operators
    //
    // *************************************

    public List<ManagedConnectorOperator> getOperators() {
        return operatorsInformer != null
            ? operatorsInformer.getIndexer().list()
            : Collections.emptyList();
    }

    // *************************************
    //
    // Cluster
    //
    // *************************************

    public Optional<ManagedConnectorCluster> getConnectorCluster() {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(this.config.operators().namespace())
                .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + getClusterId())
                .get());
    }

    public ManagedConnectorCluster getOrCreateManagedConnectorCluster() {
        return getConnectorCluster().orElseGet(() -> {
            var cluster = new ManagedConnectorClusterBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + getClusterId())
                    .addToLabels(Resources.LABEL_CLUSTER_ID, getClusterId())
                    .build())
                .withSpec(new ManagedConnectorClusterSpecBuilder()
                    .withClusterId(getClusterId())
                    .build())
                .build();

            return kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(this.config.operators().namespace())
                .withName(cluster.getMetadata().getName())
                .createOrReplace(cluster);
        });
    }
}
