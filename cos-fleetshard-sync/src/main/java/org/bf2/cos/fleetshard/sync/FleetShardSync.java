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
        fleetShardClient.getOrCreateManagedConnectorCluster();
        fleetShardClient.start();

        deploymentSync.start();
        statusSync.start();
    }

    public void stop() {
        deploymentSync.stop();
        statusSync.stop();

        fleetShardClient.stop();
    }
}
