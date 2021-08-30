package org.bf2.cos.fleetshard.sync.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ConnectorClusterStatusSync {
    @Inject
    FleetManagerClient controlPlane;

    @Scheduled(every = "{cos.cluster.status.sync.interval:60s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        controlPlane.updateClusterStatus();
    }
}
