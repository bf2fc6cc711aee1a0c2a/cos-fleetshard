package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.client.ApiException;
import org.bf2.cos.fleet.manager.api.client.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorCluster;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ControlPlane {
    @Inject
    @RestClient
    ConnectorClustersAgentApi controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    public void updateClusterStatus(ConnectorCluster cluster) {
        try {
            controlPlane.updateKafkaConnectorClusterStatus(
                    cluster.getSpec().getId(),
                    cluster.getStatus());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ConnectorDeployment> getConnectors(ConnectorCluster connectorAgent) {
        // TODO: check namespaces
        // TODO: check labels
        final List<Connector> connectors = kubernetesClient.customResources(Connector.class)
                .inNamespace(connectorAgent.getMetadata().getNamespace())
                .list().getItems();

        final long gv = connectors.stream()
                .mapToLong(c -> c.getSpec().getConnectorResourceVersion())
                .max()
                .orElse(0);

        ConnectorDeploymentList list;

        try {
            list = controlPlane.listClusterAsignedConnectorDeployments(
                    connectorAgent.getSpec().getId(),
                    null,
                    null,
                    gv,
                    false);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

        // TODO: loop pages

        return list.getItems();
    }

    public void updateConnectorStatus(String agentId, String connectorId, ConnectorDeploymentStatus status) {
        try {
            controlPlane.updateConnectorDeploymentStatus(agentId, connectorId, status);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
