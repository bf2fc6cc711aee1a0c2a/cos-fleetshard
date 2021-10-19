package org.bf2.cos.fleetshard.sync.client;

import java.util.Collections;
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
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;

@ApplicationScoped
public class FleetShardClient {

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    public String getConnectorsNamespace() {
        return config.connectors().namespace();
    }

    public String getClusterId() {
        return config.cluster().id();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public Boolean delete(ManagedConnector managedConnector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withName(managedConnector.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public long getMaxDeploymentResourceRevision() {
        final List<ManagedConnector> managedConnectors = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .list()
            .getItems();

        return managedConnectors.stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);
    }

    public Optional<Secret> getSecret(ConnectorDeployment deployment) {
        return getSecretByDeploymentId(deployment.getId());
    }

    public Optional<Secret> getSecretByDeploymentId(String deploymentId) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(getConnectorsNamespace())
                .withName(Secrets.generateConnectorSecretId(deploymentId))
                .get());
    }

    public Optional<ManagedConnector> getConnectorByName(String name) {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(getConnectorsNamespace())
                .withName(name)
                .get());
    }

    public Optional<ManagedConnector> getConnectorByDeploymentId(String deploymentId) {
        return getConnectorByName(Connectors.generateConnectorId(deploymentId));
    }

    public Optional<ManagedConnector> getConnector(ConnectorDeployment deployment) {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(getConnectorsNamespace())
                .withName(Connectors.generateConnectorId(deployment.getId()))
                .get());
    }

    public List<ManagedConnector> getAllConnectors() {
        List<ManagedConnector> answer = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public AutoCloseable watchAllConnectors(Consumer<ManagedConnector> handler) {
        var answer = new AbstractWatcher<ManagedConnector>() {
            @Override
            protected Watch doWatch() {
                return kubernetesClient.resources(ManagedConnector.class)
                    .inNamespace(getConnectorsNamespace())
                    .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
                    .watch(this);
            }

            @Override
            protected void onEventReceived(Action action, ManagedConnector resource) {
                handler.accept(resource);
            }
        };

        answer.start();

        return answer;
    }

    public AutoCloseable watchAllOperators(Consumer<ManagedConnectorOperator> handler) {
        var answer = new AbstractWatcher<ManagedConnectorOperator>() {
            @Override
            protected Watch doWatch() {
                return kubernetesClient.resources(ManagedConnectorOperator.class)
                    .inNamespace(getConnectorsNamespace())
                    .watch(this);
            }

            @Override
            protected void onEventReceived(Action action, ManagedConnectorOperator resource) {
                handler.accept(resource);
            }
        };

        answer.start();

        return answer;

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

    public Secret createSecret(Secret secret) {
        return this.kubernetesClient.secrets()
            .inNamespace(getConnectorsNamespace())
            .createOrReplace(secret);
    }

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
