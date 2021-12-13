package org.bf2.cos.fleetshard.sync.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.api.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    final FleetShardSyncConfig config;
    final ConnectorClustersAgentApi controlPlane;

    public FleetManagerClient(FleetShardSyncConfig config) {
        this.config = config;

        this.controlPlane = RestClientBuilder.newBuilder()
            .baseUri(config.manager().uri())
            .register(OidcClientRequestFilter.class)
            .connectTimeout(config.manager().connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(config.manager().readTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build(ConnectorClustersAgentApi.class);
    }

    public void getDeployments(long gv, Consumer<Collection<ConnectorDeployment>> consumer) {
        FleetManagerClientHelper.run(() -> {
            LOGGER.debug("polling with gv: {}", gv);

            final AtomicInteger counter = new AtomicInteger();
            final List<ConnectorDeployment> items = new ArrayList<>();

            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                ConnectorDeploymentList list = controlPlane.getClusterAssignedConnectorDeployments(
                    config.cluster().id(),
                    Integer.toString(i),
                    null,
                    gv,
                    "false");

                if (list == null || list.getItems() == null || list.getItems().isEmpty()) {
                    LOGGER.info("No connectors for cluster {}", config.cluster().id());
                    break;
                }

                items.clear();
                items.addAll(list.getItems());
                items.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));

                consumer.accept(items);

                if (counter.addAndGet(items.size()) == list.getTotal()) {
                    break;
                }
            }
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
                config.cluster().id(),
                new ConnectorClusterStatus()
                    .phase("ready"));
        });
    }
}
