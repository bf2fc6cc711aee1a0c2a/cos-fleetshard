package org.bf2.cos.fleetshard.sync.client;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

import static org.bf2.cos.fleetshard.api.ManagedConnector.CONTEXT_DEPLOYMENT;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_RESOURCE_CONTEXT;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class FleetShardClient {
    private final KubernetesClient kubernetesClient;
    private final String clusterId;
    private final String connectorsNamespace;
    private final String operatorsNamespace;
    private final Duration informerSyncInterval;

    public FleetShardClient(
        KubernetesClient kubernetesClient,
        @ConfigProperty(name = "cos.cluster.id") String clusterId,
        @ConfigProperty(name = "cos.connectors.namespace") String connectorsNamespace,
        @ConfigProperty(name = "cos.operators.namespace") String operatorsNamespace,
        @ConfigProperty(name = "cos.connectors.informer.sync.interval", defaultValue = "1h") Duration informerSyncInterval) {

        this.kubernetesClient = kubernetesClient;
        this.clusterId = clusterId;
        this.connectorsNamespace = connectorsNamespace;
        this.operatorsNamespace = operatorsNamespace;
        this.informerSyncInterval = informerSyncInterval;
    }

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Boolean delete(ManagedConnector managedConnector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withName(managedConnector.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public long getMaxDeploymentResourceRevision() {
        final List<ManagedConnector> managedConnectors = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .list()
            .getItems();

        return managedConnectors.stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);
    }

    public Optional<Secret> getSecret(ConnectorDeployment deployment) {
        var items = kubernetesClient.secrets()
            .inNamespace(connectorsNamespace)
            .withLabel(LABEL_RESOURCE_CONTEXT, CONTEXT_DEPLOYMENT)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + deployment.getMetadata().getResourceVersion())
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple secret with id: " + deployment.getSpec().getConnectorId());
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return Optional.of(items.getItems().get(0));
        }

        return Optional.empty();
    }

    public Optional<Secret> getSecretByDeploymentIdAndRevision(String deploymentId, long revision) {
        var items = kubernetesClient.secrets()
            .inNamespace(connectorsNamespace)
            .withLabel(LABEL_RESOURCE_CONTEXT, CONTEXT_DEPLOYMENT)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deploymentId)
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + revision)
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple secret with id: " + deploymentId);
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return Optional.of(items.getItems().get(0));
        }

        return Optional.empty();
    }

    public Optional<ManagedConnector> getConnectorByName(String name) {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(this.connectorsNamespace)
                .withName(name)
                .get());
    }

    public Optional<ManagedConnector> getConnectorByDeploymentId(String deploymentId) {
        return getConnectorByName(Connectors.generateConnectorId(deploymentId));
    }

    public Optional<ManagedConnector> getConnector(ConnectorDeployment deployment) {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(connectorsNamespace)
                .withName(Connectors.generateConnectorId(deployment.getId()))
                .get());
    }

    public List<ManagedConnector> getAllConnectors() {
        List<ManagedConnector> answer = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public AutoCloseable watchAllConnectors(Watcher<ManagedConnector> watcher) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .watch(watcher);
    }

    public AutoCloseable watchAllConnectors(ResourceEventHandler<ManagedConnector> handler) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .inform(handler, informerSyncInterval.toMillis());
    }

    public AutoCloseable watchAllConnectors(Consumer<ManagedConnector> handler) {
        return watchAllConnectors(new ResourceEventHandler<>() {
            @Override
            public void onAdd(ManagedConnector connector) {
                handler.accept(connector);
            }

            @Override
            public void onUpdate(ManagedConnector oldConnector, ManagedConnector newConnector) {
                handler.accept(newConnector);
            }

            @Override
            public void onDelete(ManagedConnector connector, boolean b) {
                handler.accept(connector);
            }
        });
    }

    public ManagedConnector createConnector(ManagedConnector connector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .createOrReplace(connector);
    }

    public ManagedConnector editConnector(String name, Consumer<ManagedConnector> editor) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withName(name)
            .accept(editor);
    }

    public Secret createSecret(Secret secret) {
        return this.kubernetesClient.secrets()
            .inNamespace(connectorsNamespace)
            .createOrReplace(secret);
    }

    public List<Operator> lookupOperators() {
        return kubernetesClient.resources(ManagedConnectorOperator.class)
            .inNamespace(this.operatorsNamespace)
            .list()
            .getItems()
            .stream()
            .map(mco -> new Operator(
                mco.getMetadata().getName(),
                mco.getSpec().getType(),
                mco.getSpec().getVersion()))
            .collect(Collectors.toList());
    }

    public Optional<ManagedConnectorCluster> getConnectorCluster() {
        var items = kubernetesClient.resources(ManagedConnectorCluster.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors clusters with id: " + clusterId);
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return Optional.of(items.getItems().get(0));
        }

        return Optional.empty();
    }

    public ManagedConnectorCluster createManagedConnectorCluster() {
        ManagedConnectorCluster cluster = getConnectorCluster().orElseGet(() -> {
            return new ManagedConnectorClusterBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + uid())
                    .addToLabels(ManagedConnector.LABEL_CLUSTER_ID, clusterId)
                    .build())
                .build();
        });

        cluster.getSpec().setClusterId(clusterId);
        ;

        return kubernetesClient.resources(ManagedConnectorCluster.class)
            .inNamespace(connectorsNamespace)
            .withName(cluster.getMetadata().getName())
            .createOrReplace(cluster);
    }
}
