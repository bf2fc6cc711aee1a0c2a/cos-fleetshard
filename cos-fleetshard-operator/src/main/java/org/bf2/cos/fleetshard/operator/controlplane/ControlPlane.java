package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.client.ApiException;
import org.bf2.cos.fleet.manager.api.client.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.operator.support.ConnectorDeploymentSupport;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ControlPlane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlane.class);

    @Inject
    @RestClient
    ConnectorClustersAgentApi controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    public void updateClusterStatus(ManagedConnectorCluster cluster) {
        try {
            controlPlane.updateKafkaConnectorClusterStatus(
                    cluster.getSpec().getId(),
                    cluster.getStatus());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ConnectorDeployment> getConnectors(ManagedConnectorCluster cluster) {
        // TODO: check namespaces
        // TODO: check labels
        final List<ManagedConnector> managedConnectors = kubernetesClient.customResources(ManagedConnector.class)
                .inNamespace(cluster.getMetadata().getNamespace())
                .list().getItems();

        final long gv = managedConnectors.stream()
                .filter(c -> c.getSpec() != null && c.getSpec().getConnectorResourceVersion() != null)
                .mapToLong(c -> c.getSpec().getConnectorResourceVersion())
                .max()
                .orElse(0);

        List<ConnectorDeployment> answer = new ArrayList<>();

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                ConnectorDeploymentList list = controlPlane.listClusterAsignedConnectorDeployments(
                        cluster.getSpec().getId(),
                        Integer.toString(i),
                        null,
                        gv,
                        false);

                answer.addAll(list.getItems());

                if (list.getItems().isEmpty() || answer.size() == list.getTotal()) {
                    break;
                }
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }

        if (answer.isEmpty()) {
            LOGGER.info("No connectors for agent {}", cluster.getSpec().getId());
        }

        answer.sort(Comparator.comparingLong(ConnectorDeploymentSupport::getResourceVersion));
        return answer;
    }

    public void updateConnectorStatus(String agentId, String connectorId, ConnectorDeploymentStatus status) {
        try {
            controlPlane.updateConnectorDeploymentStatus(agentId, connectorId, status);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
