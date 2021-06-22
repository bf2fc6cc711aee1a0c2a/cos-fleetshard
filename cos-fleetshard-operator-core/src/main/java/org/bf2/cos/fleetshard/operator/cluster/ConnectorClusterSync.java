package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterSync.class);

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    FleetShardOperator operator;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;

    @Scheduled(
        every = "{cos.cluster.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        if (!operator.isRunning()) {
            return;
        }

        LOGGER.info("Sync cluster");

        final String name = ConnectorClusterSupport.clusterName(clusterId);

        controlPlane.updateClusterStatus(
            fleetShard.lookupOrCreateManagedConnectorCluster(name),
            fleetShard.lookupManagedConnectorOperators());
    }
}
