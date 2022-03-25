package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.connector.ConnectorStatusSync;
import org.bf2.cos.fleetshard.sync.connector.ResourceSync;

@ApplicationScoped
public class FleetShardSync {
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    ResourceSync resourceSync;
    @Inject
    ConnectorStatusSync statusSync;

    public void start() {
        try {
            fleetShardClient.getOrCreateManagedConnectorCluster();
            fleetShardClient.start();

            resourceSync.start();
            statusSync.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            resourceSync.stop();
            statusSync.stop();

            fleetShardClient.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
