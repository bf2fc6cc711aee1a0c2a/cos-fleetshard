package org.bf2.cos.fleetshard.operator.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FleetShardClient {
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String connectorsNamespace;
    @ConfigProperty(
        name = "cos.cluster.namespace")
    String clusterNamespace;

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getClusterNamespace() {
        return clusterNamespace;
    }

    public String getClusterId() {
        return clusterId;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    // ******************************************************
    //
    // Cluster
    //
    //  ******************************************************

    public Optional<ManagedConnectorCluster> lookupManagedConnectorCluster() {
        return lookupManagedConnectorCluster(
            clusterNamespace,
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

    public ManagedConnectorCluster lookupOrCreateManagedConnectorCluster(String name) {
        return lookupOrCreateManagedConnectorCluster(clusterNamespace, name);
    }

    public ManagedConnectorCluster lookupOrCreateManagedConnectorCluster(String namespace, String name) {
        return lookupManagedConnectorCluster(namespace, name).orElseGet(() -> {
            return kubernetesClient.customResources(ManagedConnectorCluster.class)
                .create(new ManagedConnectorClusterBuilder()
                    .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .build())
                    .withSpec(new ManagedConnectorClusterSpecBuilder()
                        .withId(clusterId)
                        .withConnectorsNamespace(connectorsNamespace)
                        .build())
                    .build());
        });
    }

    // ******************************************************
    //
    // Operators
    //
    //  ******************************************************

    public List<ManagedConnectorOperator> lookupManagedConnectorOperators() {
        return lookupManagedConnectorOperators(clusterNamespace);
    }

    public List<ManagedConnectorOperator> lookupManagedConnectorOperators(String namespace) {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .list()
            .getItems();
    }

    public List<Operator> lookupOperators() {
        return lookupOperators(clusterNamespace);
    }

    public List<Operator> lookupOperators(String namespace) {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .list()
            .getItems()
            .stream()
            .map(mco -> new Operator(
                mco.getMetadata().getName(),
                mco.getSpec().getNamespace(),
                mco.getSpec().getType(),
                mco.getSpec().getVersion(),
                mco.getSpec().getMetaService()))
            .collect(Collectors.toList());
    }

    // ******************************************************
    //
    // Connectors
    //
    //  ******************************************************

    public Boolean deleteManagedConnector(ManagedConnector managedConnector) {
        return kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(this.connectorsNamespace)
            .withName(managedConnector.getMetadata().getName())
            .delete();
    }

    public Optional<ManagedConnector> lookupManagedConnector(
        String name) {

        return Optional.ofNullable(
            kubernetesClient.customResources(ManagedConnector.class)
                .inNamespace(this.connectorsNamespace)
                .withName(name)
                .get());
    }

    public Optional<ManagedConnector> lookupManagedConnector(
        String namespace,
        String name) {

        return Optional.ofNullable(
            kubernetesClient.customResources(ManagedConnector.class).inNamespace(namespace).withName(name).get());
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

    public List<ManagedConnector> lookupManagedConnectors() {
        List<ManagedConnector> answer = kubernetesClient
            .customResources(ManagedConnector.class)
            .inNamespace(this.connectorsNamespace)
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }
}
