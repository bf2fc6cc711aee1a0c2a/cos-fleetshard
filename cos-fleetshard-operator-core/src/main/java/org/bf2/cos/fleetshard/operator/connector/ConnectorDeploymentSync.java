package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;

@ApplicationScoped
public class ConnectorDeploymentSync {
    @Inject
    FleetShardClient fleetShard;
    @Inject
    ConnectorDeploymentProvisioner provisioner;

    @Scheduled(
        every = "{cos.connectors.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllConnectorDeployments() {
        this.provisioner.submit(0L);
    }

    @Scheduled(
        every = "{cos.connectors.poll.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        this.provisioner.submit(fleetShard.getMaxDeploymentResourceRevision());
    }

    @Scheduled(
        every = "{cos.connectors.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runProvisioner() {
        this.provisioner.run();
    }

}
