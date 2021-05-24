package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.operator.fleet.FleetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to monitor the status of the cluster.
 */
@ApplicationScoped
public class ConnectorClusterMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterMonitor.class);

    @Inject
    FleetManager controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @Scheduled(
        every = "{cos.cluster.monitor.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.info("Monitor cluster (noop)");
    }
}
