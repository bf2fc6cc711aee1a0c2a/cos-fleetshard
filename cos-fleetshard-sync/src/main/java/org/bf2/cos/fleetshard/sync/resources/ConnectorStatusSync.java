package org.bf2.cos.fleetshard.sync.resources;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ConnectorStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusSync.class);
    private static final String JOB_ID = "cos.connectors.status.sync";

    @Inject
    ConnectorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    MeterRegistry registry;

    private volatile MetricsRecorder recorder;
    private volatile Instant lastResync;
    private volatile Instant lastUpdate;

    private final ConcurrentMap<NamespacedName, Instant> connectors = new ConcurrentHashMap<>();

    public void start() throws Exception {
        LOGGER.info("Starting connector status sync");

        recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + JOB_ID);

        connectorClient.watchConnectors(new ResourceEventHandler<>() {
            @Override
            public void onAdd(ManagedConnector connector) {
                connectors.put(NamespacedName.of(connector), Instant.now());
            }

            @Override
            public void onUpdate(ManagedConnector ignored, ManagedConnector connector) {
                connectors.put(NamespacedName.of(connector), Instant.now());
            }

            @Override
            public void onDelete(ManagedConnector connector, boolean deletedFinalStateUnknown) {
                connectors.remove(NamespacedName.of(connector));
            }
        });

        scheduler.schedule(
            JOB_ID,
            ConnectorStatusSyncJob.class,
            config.resources().updateInterval());
    }

    public void stop() {
        scheduler.shutdownQuietly(JOB_ID);
    }

    public void run() {
        recorder.record(this::update);
    }

    private void update() {
        final Duration resyncInterval = config.resources().resyncInterval();
        final Instant now = Instant.now();
        final boolean resync = lastResync == null || greater(lastResync, now, resyncInterval);

        if (resync) {
            for (ManagedConnector connector : connectorClient.getAllConnectors()) {
                updater.update(connector);
            }

            lastResync = now;
        } else {
            for (Map.Entry<NamespacedName, Instant> entry : connectors.entrySet()) {
                if (entry.getValue().isAfter(lastUpdate)) {
                    connectorClient.getConnector(entry.getKey()).ifPresent(updater::update);
                }
            }
        }

        lastUpdate = now;
    }

    private static boolean greater(Temporal startInclusive, Temporal endExclusive, Duration interval) {
        return Duration.between(startInclusive, endExclusive).compareTo(interval) >= 0;
    }
}
