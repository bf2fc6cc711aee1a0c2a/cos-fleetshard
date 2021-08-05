package org.bf2.cos.fleetshard.sync.client;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import org.bf2.cos.fleet.manager.api.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    ConnectorClustersAgentApi controlPlane;

    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;

    @ConfigProperty(name = "control-plane-base-url")
    URI controlPlaneUri;

    @ConfigProperty(name = "cos.manager.connect.timeout", defaultValue = "5s")
    Duration connectTimeout;

    @ConfigProperty(name = "cos.manager.read.timeout", defaultValue = "10s")
    Duration readTimeout;

    @PostConstruct
    void setUpClientClient() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        builder.baseUri(controlPlaneUri);
        builder.register(OidcClientRequestFilter.class);
        builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);

        this.controlPlane = builder.build(ConnectorClustersAgentApi.class);
    }

    public List<ConnectorDeployment> getDeployments(long gv) {
        return FleetManagerClientHelper.call(() -> {
            LOGGER.debug("polling with gv: {}", gv);

            List<ConnectorDeployment> answer = new ArrayList<>();

            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                ConnectorDeploymentList list = controlPlane.getClusterAssignedConnectorDeployments(
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

            answer.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));

            return answer;
        });
    }

    public void updateConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus status) {
        FleetManagerClientHelper.run(() -> {
            LOGGER.info("Update connector status: cluster_id={}, deployment_id={}, status={}",
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                Serialization.asJson(status));

            controlPlane.updateConnectorDeploymentStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                status);
        });
    }

    public void updateClusterStatus() {
        FleetManagerClientHelper.run(() -> {
            controlPlane.updateKafkaConnectorClusterStatus(
                this.clusterId,
                new ConnectorClusterStatus()
                    .phase("ready"));
        });
    }
}
