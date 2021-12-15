package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.connector.ConnectorDeploymentSync;
import org.bf2.cos.fleetshard.sync.connector.ConnectorStatusSync;

@ApplicationScoped
public class FleetShardSync {
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    ConnectorDeploymentSync deploymentSync;
    @Inject
    ConnectorStatusSync statusSync;

    public void start() {
        try {
            fleetShardClient.getOrCreateManagedConnectorCluster();
            fleetShardClient.start();

            deploymentSync.start();
            statusSync.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            deploymentSync.stop();
            statusSync.stop();

            fleetShardClient.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
