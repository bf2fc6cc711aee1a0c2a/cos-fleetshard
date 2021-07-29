package org.bf2.cos.fleetshard.sync.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

@ApplicationScoped
public class ConnectorDeploymentSyncScheduler {
    @Inject
    ConnectorDeploymentQueue queue;
    @Inject
    FleetShardClient connectorClient;

    @Scheduled(
        every = "{cos.connectors.poll.resync.interval:60s}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
        identity = "cos.connectors.poll.all")
    void pollAllDeployments() {
        this.queue.submitPoisonPill();
    }

    @Scheduled(
        every = "{cos.connectors.poll.interval:15s}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
        identity = "cos.connectors.poll")
    void pollDeployments() {
        this.queue.submit(connectorClient.getMaxDeploymentResourceRevision());
    }
}
