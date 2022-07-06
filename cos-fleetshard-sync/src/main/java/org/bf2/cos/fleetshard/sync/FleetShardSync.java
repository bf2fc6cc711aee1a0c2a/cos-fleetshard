package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.housekeeping.Housekeeper;
import org.bf2.cos.fleetshard.sync.resources.ConnectorClusterStatusSync;
import org.bf2.cos.fleetshard.sync.resources.ConnectorStatusSync;
import org.bf2.cos.fleetshard.sync.resources.ResourcePoll;

@ApplicationScoped
public class FleetShardSync implements Service {
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    ResourcePoll resourceSync;
    @Inject
    ConnectorStatusSync connectorStatusSync;
    @Inject
    ConnectorClusterStatusSync clusterStatusSync;
    @Inject
    Housekeeper housekeeping;

    @Override
    public void start() throws Exception {
        fleetShardClient.setupObservability();
        fleetShardClient.getOrCreateManagedConnectorCluster();
        fleetShardClient.start();

        startResourcesSync();

        housekeeping.start();
    }

    @Override
    public void stop() throws Exception {
        stopResourcesSync();

        Resources.closeQuietly(housekeeping);
        Resources.closeQuietly(fleetShardClient);
    }

    public void startResourcesSync() throws Exception {
        resourceSync.start();
        connectorStatusSync.start();
        clusterStatusSync.start();
    }

    public void stopResourcesSync() throws Exception {
        Resources.closeQuietly(resourceSync);
        Resources.closeQuietly(connectorStatusSync);
        Resources.closeQuietly(clusterStatusSync);
    }

}
