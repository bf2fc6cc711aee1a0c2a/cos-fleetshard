package org.bf2.cos.fleetshard.sync.resources;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ResourcePoll implements Service {
    private static final String JOB_ID = "cos.resources.poll";
    private static final long BEGINNING = 0;
    public static final String METRICS_SYNC = "connectors.sync";
    public static final String METRICS_POLL = "connectors.poll";

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

    private volatile MetricsRecorder syncRecorder;
    private volatile MetricsRecorder pollRecorder;
    private volatile Instant lastResync;

    @Override
    public void start() throws Exception {
        syncRecorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + METRICS_SYNC);
        pollRecorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + METRICS_POLL);

        scheduler.schedule(
            JOB_ID,
            ResourcePollJob.class,
            config.resources().pollInterval());
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownQuietly(JOB_ID);
    }

    @Retry(maxRetries = 10, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public void run() {
        Instant now = Instant.now();
        boolean resync = lastResync == null;

        if (lastResync != null) {
            resync = Duration.between(lastResync, now).compareTo(config.resources().resyncInterval()) > 0;
        }

        if (resync) {
            syncRecorder.record(this::sync);
            lastResync = now;
        } else {
            pollRecorder.record(this::poll);
        }
    }

    private void sync() {
        namespaceProvisioner.poll(BEGINNING);
        connectorsProvisioner.poll(BEGINNING);
    }

    private void poll() {
        namespaceProvisioner.poll(
            connectorClient.getMaxNamespaceResourceRevision());
        connectorsProvisioner.poll(
            connectorClient.getMaxDeploymentResourceRevision());
    }
}
