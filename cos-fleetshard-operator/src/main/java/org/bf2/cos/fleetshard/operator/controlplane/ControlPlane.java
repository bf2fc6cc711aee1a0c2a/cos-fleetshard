package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleet.manager.api.client.cp.ApiException;
import org.bf2.cos.fleet.manager.api.client.cp.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ControlPlane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlane.class);

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String namespace;

    @Inject
    @RestClient
    ConnectorClustersAgentApi controlPlane;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Config cfg;

    public void updateClusterStatus(ManagedConnectorCluster cluster) {
        try {
            var status = new ConnectorClusterStatus();
            status.setPhase(cluster.getStatus().getPhase().name().toLowerCase(Locale.US));

            LOGGER.info("Update Cluster Status {}", status);

            for (String name : cfg.getPropertyNames()) {
                if (name.startsWith("cos.") || name.startsWith("rh.")) {
                    LOGGER.debug("cfg> {} = {}", name, cfg.getOptionalValue(name, String.class).orElse(""));
                }
            }

            controlPlane.updateKafkaConnectorClusterStatus(clusterId, status);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectorDeployment getDeployment(String clusterId, String deploymentId) {
        try {
            return controlPlane.getClusterAsignedConnectorDeployments(clusterId, deploymentId);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ConnectorDeployment> getDeployments() {
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

        LOGGER.debug("gv {}", gv);

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                ConnectorDeploymentList list = controlPlane.listClusterAsignedConnectorDeployments(
                    clusterId,
                    Integer.toString(i),
                    null,
                    gv,
                    "false");

                LOGGER.info("Got: {}", list);
                if (list == null || list.getItems() == null) {
                    break;
                }

                answer.addAll(list.getItems());

                if (list.getItems().isEmpty() || answer.size() == list.getTotal()) {
                    break;
                }
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

    public void updateConnectorStatus(String clusterId, String connectorId, ConnectorDeploymentStatus status) {
        try {
            LOGGER.info("Connector clusterId={}, id={}, status={}", clusterId, connectorId, Serialization.asJson(status));
            controlPlane.updateConnectorDeploymentStatus(clusterId, connectorId, status);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
