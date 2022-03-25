package org.bf2.cos.fleetshard.sync.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ConnectorClusterStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterStatusSync.class);
    private static final String JOB_ID = "cluster.status.sync";

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    MeterRegistry registry;

    private volatile MetricsRecorder recorder;

    public void start() throws Exception {
        LOGGER.info("Starting connector status sync");

        recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + JOB_ID);

        scheduler.schedule(
            JOB_ID,
            ConnectorClusterStatusSyncJob.class,
            config.resources().updateInterval());
    }

    public void stop() {
        scheduler.shutdownQuietly(JOB_ID);
    }

    public void run() {
        recorder.record(this::update);
    }

    private void update() {
        controlPlane.updateClusterStatus(
            fleetShardClient.getOperators(),
            fleetShardClient.getNamespaces());
    }
}
