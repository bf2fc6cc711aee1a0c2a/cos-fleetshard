package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

@ApplicationScoped
public class FleetShardSync {
    @Inject
    FleetShardClient client;

    public void start() {
        client.createManagedConnectorCluster();
    }

    public void stop() {
    }
}
