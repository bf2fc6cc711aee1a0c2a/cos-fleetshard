package org.bf2.cos.fleetshard.sync.resources;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.metrics.MetricsSupport;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class ConnectorStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusUpdater.class);

    public static final String CONNECTOR_STATE = "connector.state";
    public static final String CONNECTOR_STATE_COUNT = "connector.state.count";

    static final int CONNECTOR_STATE_READY = 1;
    static final int CONNECTOR_STATE_FAILED = 2;
    static final int CONNECTOR_STATE_DELETED = 3;
    static final int CONNECTOR_STATE_STOPPED = 4;
    static final int CONNECTOR_STATE_IN_PROCESS = 5;

    @Inject
    FleetManagerClient fleetManagerClient;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    MeterRegistry registry;
    @Inject
    FleetShardSyncConfig config;

    public void update(ManagedConnector connector) {
        LOGGER.debug("Update connector status (name: {}, phase: {})",
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        try {
            ConnectorDeploymentStatus connectorDeploymentStatus = ConnectorStatusExtractor.extract(connector);

            fleetManagerClient.updateConnectorStatus(connector, connectorDeploymentStatus);

            LOGGER.debug("Updating Connector status metrics (Connector_id: {}, state: {})",
                connector.getSpec().getConnectorId(), connectorDeploymentStatus.getPhase());

            switch (connectorDeploymentStatus.getPhase()) {
                case READY:
                    measure(connector, connectorDeploymentStatus, CONNECTOR_STATE_READY);
                    break;
                case FAILED:
                    measure(connector, connectorDeploymentStatus, CONNECTOR_STATE_FAILED);
                    break;
                case DELETED:
                    measure(connector, connectorDeploymentStatus, CONNECTOR_STATE_DELETED);
                    break;
                case STOPPED:
                    measure(connector, connectorDeploymentStatus, CONNECTOR_STATE_STOPPED);
                    break;
                default:
                    measure(connector, connectorDeploymentStatus, CONNECTOR_STATE_IN_PROCESS);
                    break;
            }

        } catch (FleetManagerClientException e) {
            if (e.getStatusCode() == 410) {
                LOGGER.info("Connector " + connector.getMetadata().getName() + " does not exists anymore, deleting it");
                if (connectorClient.deleteConnector(connector)) {
                    LOGGER.info("Connector " + connector.getMetadata().getName() + " deleted");
                }
            } else {
                LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
        }
    }

    /*
     * Expose a Gauge metric "cos_fleetshard_sync_connector_state" which reveals the current connector state.
     * Metric value of 1 implies that the connector is in Ready state. Similarly, 2 -> Failed, 3 -> Deleted,
     * 4 -> Stopped, 5 -> In Process
     * Also exposing a Counter metrics "cos_fleetshard_sync_connector_state_count_total" which reveals each
     * state count for the connector
     */
    private void measure(ManagedConnector connector, ConnectorDeploymentStatus connectorDeploymentStatus, int connectorState) {

        List<Tag> tags = MetricsSupport.tags(config.metrics().recorder(), connector);
        tags.add(Tag.of("cos.connector.id", connector.getSpec().getConnectorId()));
        tags.add(Tag.of("cos.connector.type.id", connector.getSpec().getDeployment().getConnectorTypeId()));
        tags.add(Tag.of("cos.deployment.id", connector.getSpec().getDeploymentId()));
        tags.add(Tag.of("cos.namespace", connector.getMetadata().getNamespace()));

        String connectorResourceVersion = String.valueOf(connector.getSpec().getDeployment().getConnectorResourceVersion());

        Gauge gauge = registry.find(config.metrics().baseName() + "." + CONNECTOR_STATE).tags(tags).gauge();

        if (gauge != null) {
            registry.remove(gauge);
        }

        Gauge.builder(config.metrics().baseName() + "." + CONNECTOR_STATE, () -> new AtomicInteger(connectorState))
            .tags(tags)
            .register(registry);

        Counter.builder(config.metrics().baseName() + "." + CONNECTOR_STATE_COUNT)
            .tags(tags)
            .tag("cos.connector.state", connectorDeploymentStatus.getPhase().getValue())
            .tag("cos.connector.resourceversion", connectorResourceVersion)
            .register(registry)
            .increment();

        if (CONNECTOR_STATE_FAILED == connectorState) {
            Counter counter = registry.find(config.metrics().baseName() + "." + CONNECTOR_STATE_COUNT)
                .tags(tags).tag("cos.connector.state", "ready")
                .tagKeys("cos.connector.resourceversion").counter();

            if (counter != null && counter.count() != 0) {

                // Exposing a new state "failed_but_ready" when a connector has already started but now failing
                Counter.builder(config.metrics().baseName() + "." + CONNECTOR_STATE_COUNT)
                    .tags(tags)
                    .tag("cos.connector.state", "failed_but_ready")
                    .tag("cos.connector.resourceversion", connectorResourceVersion)
                    .register(registry)
                    .increment();
            }
        }

    }
}
