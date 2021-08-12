package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

@ApplicationScoped
public class FleetShardSync {
    @Inject
    FleetShardClient client;

    void onStart(@Observes StartupEvent ignored) {
        client.createManagedConnectorCluster();
    }
}
