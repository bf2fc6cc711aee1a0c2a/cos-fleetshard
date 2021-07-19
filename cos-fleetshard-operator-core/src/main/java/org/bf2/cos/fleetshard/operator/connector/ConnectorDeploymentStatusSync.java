package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentStatusSync.class);

    @Inject
    FleetShardOperator operator;
    @Inject
    ConnectorDeploymentStatusUpdater sync;

    @Scheduled(
        every = "{cos.connectors.status.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllConnectorDeploymentStatus() {
        if (!operator.isRunning()) {
            return;
        }

        this.sync.submit((String) null);
    }

    @Scheduled(
        every = "{cos.connectors.status.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runUpdater() {
        if (!operator.isRunning()) {
            return;
        }

        this.sync.run();
    }

}
