package org.bf2.cos.fleetshard.sync.client;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.support.watch.Informers;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

@ApplicationScoped
public class FleetShardClient {

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    private volatile SharedIndexInformer<ManagedConnector> informer;

    public void start() {
        informer = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .inform();
    }

    public void stop() {
        if (informer != null) {
            informer.stop();
        }
    }

    public String getConnectorsNamespace() {
        return config.connectors().namespace();
    }

    public String getClusterId() {
        return config.cluster().id();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public long getMaxDeploymentResourceRevision() {
        return this.informer.getIndexer().list().stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);
    }

    // *************************************
    //
    // Secrets
    //
    // *************************************

    public Optional<Secret> getSecret(ConnectorDeployment deployment) {
        return getSecretByDeploymentId(deployment.getId());
    }

    public Secret createSecret(Secret secret) {
        return this.kubernetesClient.secrets()
            .inNamespace(getConnectorsNamespace())
            .createOrReplace(secret);
    }

    public Optional<Secret> getSecretByDeploymentId(String deploymentId) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(getConnectorsNamespace())
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
            .inNamespace(getConnectorsNamespace())
            .withName(managedConnector.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public Optional<ManagedConnector> getConnectorByName(String name) {
        if (informer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        final String key = getConnectorsNamespace() + "/" + name;
        final ManagedConnector val = informer.getIndexer().getByKey(key);

        return Optional.ofNullable(val);
    }

    public Optional<ManagedConnector> getConnectorByDeploymentId(String deploymentId) {
        return getConnectorByName(Connectors.generateConnectorId(deploymentId));
    }

    public Optional<ManagedConnector> getConnector(ConnectorDeployment deployment) {
        return getConnectorByName(Connectors.generateConnectorId(deployment.getId()));
    }

    public List<ManagedConnector> getAllConnectors() {
        if (informer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        return informer.getIndexer().list();
    }

    public void watchConnectors(Consumer<ManagedConnector> handler) {
        if (informer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        informer.addEventHandler(Informers.wrap(handler));
    }

    public ManagedConnector createConnector(ManagedConnector connector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .createOrReplace(connector);
    }

    public ManagedConnector editConnector(String name, Consumer<ManagedConnector> editor) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withName(name)
            .accept(editor);
    }

    // *************************************
    //
    // Operators
    //
    // *************************************

    public List<Operator> lookupOperators() {
        return kubernetesClient.resources(ManagedConnectorOperator.class)
            .inNamespace(this.getConnectorsNamespace())
            .list()
            .getItems()
            .stream()
            .map(mco -> new Operator(
                mco.getMetadata().getName(),
                mco.getSpec().getType(),
                mco.getSpec().getVersion()))
            .collect(Collectors.toList());
    }

    // *************************************
    //
    // Cluster
    //
    // *************************************

    public Optional<ManagedConnectorCluster> getConnectorCluster() {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(getConnectorsNamespace())
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
                .inNamespace(getConnectorsNamespace())
                .withName(cluster.getMetadata().getName())
                .createOrReplace(cluster);
        });
    }
}
