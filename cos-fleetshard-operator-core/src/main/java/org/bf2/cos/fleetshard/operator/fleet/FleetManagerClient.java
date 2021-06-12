package org.bf2.cos.fleetshard.operator.fleet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.client.cp.ApiException;
import org.bf2.cos.fleet.manager.api.client.cp.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    @Inject
    @RestClient
    ConnectorClustersAgentApi controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    public void updateClusterStatus(ManagedConnectorCluster cluster) {
        try {
            var phase = cluster.getStatus().getPhase();
            if (phase == null) {
                phase = ManagedConnectorClusterStatus.PhaseType.Unconnected;
            }

            var status = new ConnectorClusterStatus();
            status.setPhase(phase.name().toLowerCase(Locale.US));

            controlPlane.updateKafkaConnectorClusterStatus(cluster.getSpec().getId(), status);
        } catch (javax.ws.rs.WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class));
            throw new RuntimeException(e);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectorDeployment getDeployment(String clusterId, String deploymentId) {
        try {
            return controlPlane.getClusterAsignedConnectorDeploymentById(clusterId, deploymentId);
        } catch (javax.ws.rs.WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class));
            throw new RuntimeException(e);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ConnectorDeployment> getDeployments(String clusterId, String namespace) {
        // TODO: check namespaces
        // TODO: check labels
        final List<ManagedConnector> managedConnectors = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .list().getItems();

        final long gv = managedConnectors.stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);

        List<ConnectorDeployment> answer = new ArrayList<>();

        LOGGER.debug("polling with gv: {}", gv);

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                ConnectorDeploymentList list = controlPlane.getClusterAsignedConnectorDeployments(
                    clusterId,
                    Integer.toString(i),
                    null,
                    gv,
                    "false");

                if (list == null || list.getItems() == null) {
                    break;
                }

                answer.addAll(list.getItems());

                if (list.getItems().isEmpty() || answer.size() == list.getTotal()) {
                    break;
                }
            } catch (javax.ws.rs.WebApplicationException e) {
                LOGGER.warn("{}", e.getResponse().readEntity(Error.class));
                throw new RuntimeException(e);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }

        if (answer.isEmpty()) {
            LOGGER.info("No connectors for agent {}", clusterId);
        }

        answer.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));
        return answer;
    }

    public void updateConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus status) {
        try {
            controlPlane.updateConnectorDeploymentStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                status);
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
