package org.bf2.cos.fleetshard.operator.fleet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FleetShardClient {
    private final KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String connectorsNamespace;

    public FleetShardClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public Optional<ManagedConnectorCluster> lookupManagedConnectorCluster() {
        return lookupManagedConnectorCluster(
            kubernetesClient.getNamespace(),
            ConnectorClusterSupport.clusterName(clusterId));
    }

    public Optional<ManagedConnectorCluster> lookupManagedConnectorCluster(String namespace) {
        return lookupManagedConnectorCluster(
            namespace,
            ConnectorClusterSupport.clusterName(clusterId));
    }

    public Optional<ManagedConnectorCluster> lookupManagedConnectorCluster(String namespace, String name) {
        return Optional.ofNullable(
            kubernetesClient.customResources(ManagedConnectorCluster.class)
                .inNamespace(namespace)
                .withName(name)
                .get());
    }

    public List<ManagedConnectorCluster> lookupManagedConnectorClusters(String namespace) {
        return kubernetesClient.customResources(ManagedConnectorCluster.class)
            .inNamespace(namespace)
            .list()
            .getItems();

    }

    public Optional<ManagedConnector> lookupManagedConnector(
        ManagedConnectorCluster connectorCluster,
        ConnectorDeployment deployment) {

        return lookupManagedConnector(connectorCluster.getSpec().getConnectorsNamespace(), deployment);
    }

    public Optional<ManagedConnector> lookupManagedConnector(String namespace, ConnectorDeployment deployment) {
        var items = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors with id " + deployment.getSpec().getConnectorId());
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return Optional.of(items.getItems().get(0));
        }

        return Optional.empty();
    }

    public List<ManagedConnectorOperator> lookupManagedConnectorOperators() {
        return lookupManagedConnectorOperators(kubernetesClient.getNamespace());
    }

    public List<ManagedConnectorOperator> lookupManagedConnectorOperators(String namespace) {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .list()
            .getItems();
    }

    public List<Operator> lookupOperators() {
        return lookupOperators(kubernetesClient.getNamespace());
    }

    public List<Operator> lookupOperators(String namespace) {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .list()
            .getItems()
            .stream()
            .map(ManagedConnectorOperator::getSpec)
            .collect(Collectors.toList());
    }
}
