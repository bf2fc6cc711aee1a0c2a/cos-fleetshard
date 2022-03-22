package org.bf2.cos.fleetshard.sync.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ConnectorClusterStatusSync {
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShardClient;

    @Scheduled(every = "{cos.cluster.status.sync-interval:30s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        controlPlane.updateClusterStatus(
            fleetShardClient.getOperators(),
            fleetShardClient.getNamespaces());
    }
}
