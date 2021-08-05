package org.bf2.cos.fleetshard.sync.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ConnectorStatusSyncScheduler {
    @Inject
    ConnectorStatusQueue queue;

    @Scheduled(every = "{cos.connectors.status.resync.interval:60s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllDeployments() {
        this.queue.submitPoisonPill();
    }
}
