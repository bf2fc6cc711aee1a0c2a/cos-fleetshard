package org.bf2.cos.fleetshard.sync.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusUpdater.class);

    @Inject
    FleetManagerClient fleetManagerClient;
    @Inject
    FleetShardClient connectorClient;

    public void update(ManagedConnector connector) {
        LOGGER.debug("Update connector status (name: {}, phase: {})",
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        try {
            fleetManagerClient.updateConnectorStatus(
                connector,
                ConnectorStatusExtractor.extract(connector));

        } catch (FleetManagerClientException e) {
            // TODO: remove 404 after https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-manager/issues/2
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                LOGGER.info("Connector " + connector.getMetadata().getName() + " does not exists anymore, deleting it");
                if (connectorClient.delete(connector)) {
                    LOGGER.info("Connector " + connector.getMetadata().getName() + " deleted");
                }
            } else {
                LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
        }
    }

}
