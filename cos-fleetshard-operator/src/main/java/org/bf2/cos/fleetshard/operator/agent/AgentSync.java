package org.bf2.cos.fleetshard.operator.agent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class AgentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSync.class);

    @Inject
    ControlPlane controlPlane;

    @Scheduled(every = "{cos.agent.sync.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.info("Sync agent");
    }
}
