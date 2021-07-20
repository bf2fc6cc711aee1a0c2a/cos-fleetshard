package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ConnectorDeploymentStatusSync {
    @Inject
    ConnectorDeploymentStatusUpdater sync;

    @Scheduled(
        every = "{cos.connectors.status.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllConnectorDeploymentStatus() {
        this.sync.submit((String) null);
    }

    @Scheduled(
        every = "{cos.connectors.status.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runUpdater() {
        this.sync.run();
    }

}
