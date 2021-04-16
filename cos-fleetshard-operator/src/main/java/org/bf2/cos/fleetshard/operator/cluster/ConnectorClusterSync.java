package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterSync.class);

    @Inject
    ControlPlane controlPlane;

    @Timed(
        value = "cos.agent.sync.poll",
        extraTags = { "resource", "ManagedConnectorsAgent" },
        description = "The time spent processing polling calls")
    @Counted(
        value = "cos.agent.sync.poll",
        extraTags = { "resource", "ManagedConnectorsAgent" },
        description = "The number of polling calls")
    @Scheduled(
        every = "{cos.agent.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.info("Sync agent (noop)");
    }
}
