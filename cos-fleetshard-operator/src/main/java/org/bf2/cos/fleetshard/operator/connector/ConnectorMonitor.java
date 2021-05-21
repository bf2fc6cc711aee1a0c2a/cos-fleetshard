package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to monitor connectors.
 */
@ApplicationScoped
public class ConnectorMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorMonitor.class);

    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @Scheduled(
        every = "{cos.connectors.monitor.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void monitor() {
        LOGGER.debug("Monitor connectors (noop)");
    }
}
