package org.bf2.cos.fleetshard.operator.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleet.manager.api.client.cp.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatusOperators;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.OperatorSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.operator.client.FleetManagerClientHelper.call;
import static org.bf2.cos.fleetshard.operator.client.FleetManagerClientHelper.run;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;

    @Inject
    @RestClient
    ConnectorClustersAgentApi controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    public void updateClusterStatus(ManagedConnectorCluster cluster, List<ManagedConnectorOperator> operators) {
        run(() -> {
            var phase = cluster.getStatus().getPhase();
            if (phase == null) {
                phase = ManagedConnectorClusterStatus.PhaseType.Unconnected;
            }

            var ops = operators.stream()
                .map(o -> new ConnectorClusterStatusOperators()
                    .operator(OperatorSupport.toConnectorOperator(o))
                    .namespace(o.getMetadata().getName()))
                .collect(Collectors.toList());

            controlPlane.updateKafkaConnectorClusterStatus(
                cluster.getSpec().getId(),
                new ConnectorClusterStatus()
                    .phase(phase.name().toLowerCase(Locale.US))
                    .operators(ops));
        });
    }

    public ConnectorDeployment getDeployment(String clusterId, String deploymentId) {
        return call(() -> {
            return controlPlane.getClusterAsignedConnectorDeploymentById(clusterId, deploymentId);
        });
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

        run(() -> {
            LOGGER.debug("polling with gv: {}", gv);

            for (int i = 0; i < Integer.MAX_VALUE; i++) {
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
            }

            if (answer.isEmpty()) {
                LOGGER.info("No connectors for agent {}", clusterId);
            }
        });

        answer.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));
        return answer;
    }

    public void updateConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus status) {
        run(() -> {
            controlPlane.updateConnectorDeploymentStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                status);
        });
    }
}
