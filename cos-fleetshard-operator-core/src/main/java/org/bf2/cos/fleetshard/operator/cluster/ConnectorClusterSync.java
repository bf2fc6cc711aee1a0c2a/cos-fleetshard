package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterSync {
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;

    @Scheduled(
        every = "{cos.cluster.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        controlPlane.updateClusterStatus(
            fleetShard.lookupOrCreateManagedConnectorCluster(),
            fleetShard.lookupManagedConnectorOperators());
    }
}
