package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.housekeeping.Housekeeper;
import org.bf2.cos.fleetshard.sync.resources.AddonCleanup;
import org.bf2.cos.fleetshard.sync.resources.AddonConfigMapWatcher;
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
    @Inject
    Housekeeper housekeeping;
    @Inject
    AddonCleanup addonCleanup;
    @Inject
    AddonConfigMapWatcher addonConfigMapWatcher;

    public void start() {
        try {
            fleetShardClient.getOrCreateManagedConnectorCluster();
            fleetShardClient.start();

            resourceSync.start();
            statusSync.start();
            clusterStatusSync.start();
            addonConfigMapWatcher.start();

            housekeeping.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            housekeeping.stop();

            resourceSync.stop();
            statusSync.stop();
            clusterStatusSync.stop();

            fleetShardClient.stop();
            addonConfigMapWatcher.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startCleanup() {
        this.stop();
        try {
            addonCleanup.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopCleanup() {
        addonCleanup.stop();
    }
}
