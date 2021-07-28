package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterStatusSync {
    @Inject
    ConnectorClusterStatusUpdater sync;

    @Scheduled(
        every = "{cos.cluster.status.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        this.sync.poison();
    }

    @Scheduled(
        every = "{cos.cluster.status.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runUpdater() {
        this.sync.run();
    }
}
