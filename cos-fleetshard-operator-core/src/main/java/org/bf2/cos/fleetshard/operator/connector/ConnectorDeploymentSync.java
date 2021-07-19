package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentSync.class);

    @Inject
    FleetShardClient fleetShard;
    @Inject
    ConnectorDeploymentProvisioner provisioner;
    @Inject
    FleetShardOperator operator;

    @Scheduled(
        every = "{cos.connectors.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllConnectorDeployments() {
        if (!operator.isRunning()) {
            return;
        }

        this.provisioner.submit(0L);
    }

    @Scheduled(
        every = "{cos.connectors.poll.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        if (!operator.isRunning()) {
            return;
        }

        this.provisioner.submit(fleetShard.getMaxDeploymentResourceRevision());
    }

    @Scheduled(
        every = "{cos.connectors.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runProvisioner() {
        if (!operator.isRunning()) {
            return;
        }

        this.provisioner.run();
    }

}
