package org.bf2.cos.fleetshard.sync.resources;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class ConnectorStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusUpdater.class);

    public static final String CONNECTOR_STATE = "connector.state";

    static final Integer CONNECTOR_STATE_READY = 1;
    static final Integer CONNECTOR_STATE_FAILED = 2;
    static final Integer CONNECTOR_STATE_DELETED = 3;
    static final Integer CONNECTOR_STATE_STOPPED = 4;
    static final Integer CONNECTOR_STATE_IN_PROCESS = 5;

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

            LOGGER.debug("Updating Connector status metrics (Connector_id : {}, state: {})",
                connector.getSpec().getConnectorId(),
                ConnectorStatusExtractor.extract(connector).getPhase());

            switch (connectorDeploymentStatus.getPhase()) {
                case READY:
                    measure(connector, CONNECTOR_STATE_READY);
                    break;
                case FAILED:
                    measure(connector, CONNECTOR_STATE_FAILED);
                    break;
                case DELETED:
                    measure(connector, CONNECTOR_STATE_DELETED);
                    break;
                case STOPPED:
                    measure(connector, CONNECTOR_STATE_STOPPED);
                    break;
                default:
                    measure(connector, CONNECTOR_STATE_IN_PROCESS);
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
     * Expose a metric "cos_fleetshard_sync_connector_state" which reveals the current connector state.
     * Metric value of 1 implies that the connector is in Ready state. Similarly, 2 -> Failed, 3 -> Deleted,
     * 4 -> Stopped, 5 -> In Process
     */
    private void measure(ManagedConnector connector, Integer connectorState) {

        List<Tag> tags = List.of(
            Tag.of("cos.connector.id", connector.getSpec().getConnectorId()),
            Tag.of("cos.deployment.id", connector.getSpec().getDeploymentId()));

        Gauge gauge = registry.find(config.metrics().baseName() + "." + CONNECTOR_STATE).tags(tags).gauge();

        if (gauge != null) {
            registry.remove(gauge);
        }

        Gauge.builder(config.metrics().baseName() + "." + CONNECTOR_STATE, () -> new AtomicInteger(connectorState))
            .tags(tags)
            .register(registry);

    }
}
