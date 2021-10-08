package org.bf2.cos.fleetshard.sync.client;

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
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;

@ApplicationScoped
public class FleetShardClient {
    private final KubernetesClient kubernetesClient;
    private final String clusterId;
    private final String connectorsNamespace;
    private final String operatorsNamespace;

    public FleetShardClient(
        KubernetesClient kubernetesClient,
        @ConfigProperty(name = "cos.cluster.id") String clusterId,
        @ConfigProperty(name = "cos.connectors.namespace") String connectorsNamespace,
        @ConfigProperty(name = "cos.operators.namespace") String operatorsNamespace) {

        this.kubernetesClient = kubernetesClient;
        this.clusterId = clusterId;
        this.connectorsNamespace = connectorsNamespace;
        this.operatorsNamespace = operatorsNamespace;
    }

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getClusterId() {
        return clusterId;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
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
            .withLabel(Resources.LABEL_CLUSTER_ID, clusterId)
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
                .inNamespace(connectorsNamespace)
                .withName(Secrets.generateConnectorSecretId(deploymentId))
                .get());
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
            .withLabel(Resources.LABEL_CLUSTER_ID, clusterId)
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public AutoCloseable watchAllConnectors(Consumer<ManagedConnector> handler) {
        var answer = new AbstractWatcher<ManagedConnector>() {
            @Override
            protected Watch doWatch() {
                return kubernetesClient.resources(ManagedConnector.class)
                    .inNamespace(connectorsNamespace)
                    .withLabel(Resources.LABEL_CLUSTER_ID, clusterId)
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
                    .inNamespace(operatorsNamespace)
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
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(connectorsNamespace)
                .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + clusterId)
                .get());
    }

    public ManagedConnectorCluster getOrCreateManagedConnectorCluster() {
        return getConnectorCluster().orElseGet(() -> {
            var cluster = new ManagedConnectorClusterBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + clusterId)
                    .addToLabels(Resources.LABEL_CLUSTER_ID, clusterId)
                    .build())
                .withSpec(new ManagedConnectorClusterSpecBuilder()
                    .withClusterId(clusterId)
                    .build())
                .build();

            return kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(connectorsNamespace)
                .withName(cluster.getMetadata().getName())
                .createOrReplace(cluster);
        });
    }
}
