package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.resources.ConnectorClusterStatusSync;
import org.bf2.cos.fleetshard.sync.resources.ConnectorStatusSync;
import org.bf2.cos.fleetshard.sync.resources.ResourcePoll;

@ApplicationScoped
public class FleetShardSync {
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    ResourcePoll resourceSync;
    @Inject
    ConnectorStatusSync statusSync;
    @Inject
    ConnectorClusterStatusSync clusterStatusSync;

    public void start() {
        try {
            fleetShardClient.getOrCreateManagedConnectorCluster();
            fleetShardClient.start();

            resourceSync.start();
            statusSync.start();
            clusterStatusSync.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            resourceSync.stop();
            statusSync.stop();
            clusterStatusSync.stop();

            fleetShardClient.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
