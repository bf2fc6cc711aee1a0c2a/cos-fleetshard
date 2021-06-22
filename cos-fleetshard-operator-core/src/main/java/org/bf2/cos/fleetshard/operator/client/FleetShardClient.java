package org.bf2.cos.fleetshard.operator.client;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.api.model.cp.MetaV1Condition;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentStatusRequest;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.support.OperatorSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FleetShardClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardClient.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    MetaClient meta;
    @Inject
    UnstructuredClient uc;

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

    public List<ManagedConnector> lookupConnectors() {
        List<ManagedConnector> answer = kubernetesClient
            .customResources(ManagedConnector.class)
            .inNamespace(this.connectorsNamespace)
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public ConnectorDeploymentStatus getConnectorDeploymentStatus(ManagedConnector connector) {
        ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
        ds.setResourceVersion(connector.getStatus().getDeployment().getDeploymentResourceVersion());

        setConnectorOperators(connector, ds);
        setConnectorStatus(connector, ds);
        return ds;
    }

    private void setConnectorOperators(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        // report available operators
        deploymentStatus.setOperators(
            new ConnectorDeploymentStatusOperators()
                .assigned(OperatorSupport.toConnectorOperator(connector.getStatus().getAssignedOperator()))
                .available(OperatorSupport.toConnectorOperator(connector.getStatus().getAvailableOperator())));
    }

    private void setConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        ConnectorDeploymentStatusRequest sr = new ConnectorDeploymentStatusRequest()
            .managedConnectorId(connector.getMetadata().getName())
            .deploymentId(connector.getSpec().getDeploymentId())
            .connectorId(connector.getSpec().getConnectorId())
            .connectorTypeId(connector.getSpec().getConnectorTypeId());

        for (DeployedResource resource : connector.getStatus().getResources()) {
            // don't include secrets ...
            if (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Secret", resource.getKind())) {
                continue;
            }

            sr.addResourcesItem(
                uc.getAsNode(connector.getMetadata().getNamespace(), resource));
        }

        if (connector.getStatus().getAssignedOperator() != null) {
            var answer = meta.status(
                connector.getStatus().getAssignedOperator().getMetaService(),
                sr);

            deploymentStatus.setPhase(answer.getPhase());

            // TODO: fix model duplications
            if (answer.getConditions() != null) {
                for (var cond : answer.getConditions()) {
                    deploymentStatus.addConditionsItem(
                        new MetaV1Condition()
                            .type(cond.getType())
                            .status(cond.getStatus())
                            .message(cond.getMessage())
                            .reason(cond.getReason())
                            .lastTransitionTime(cond.getLastTransitionTime()));
                }
            }
        } else {
            deploymentStatus.setPhase("provisioning");
        }
    }
}
