package org.bf2.cos.fleetshard.sync.connector;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ResourceSync {
    private static final String JOB_ID = "cos.resources.poll";
    private static final long BEGINNING = 0;

    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    ConnectorDeploymentProvisioner connectorsProvisioner;
    @Inject
    ConnectorNamespaceProvisioner namespaceProvisioner;
    @Inject
    MeterRegistry registry;

    private volatile MetricsRecorder recorder;
    private volatile Instant lastResync;

    public void start() throws Exception {
        scheduler.schedule(
            JOB_ID,
            ResourceSyncJob.class,
            config.resources().pollInterval());
    }

    public void stop() {
        try {
            scheduler.shutdown(JOB_ID);
        } catch (Exception ignored) {
        }
    }

    @Retry(maxRetries = 10, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public void sync() {
        if (recorder == null) {
            recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + JOB_ID);
        }

        recorder.record(this::poll);
    }

    private void poll() {
        Instant now = Instant.now();
        boolean resync = lastResync == null;

        if (lastResync != null) {
            resync = Duration.between(lastResync, now).compareTo(config.resources().resyncInterval()) > 0;
        }

        if (resync) {
            namespaceProvisioner.poll(BEGINNING);
            connectorsProvisioner.poll(BEGINNING);

            lastResync = now;
        } else {
            namespaceProvisioner.poll(
                connectorClient.getMaxNamespaceResourceRevision());
            connectorsProvisioner.poll(
                connectorClient.getMaxDeploymentResourceRevision());
        }
    }

}
