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
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.metrics.MetricsConfig;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.StaticMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.micrometer.core.instrument.Counter;

@ApplicationScoped
public class ConnectorStatusSync implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusSync.class);

    public static final String JOB_ID = "cos.connectors.status.sync";
    public static final String METRICS_SYNC = "connectors.status.sync";
    public static final String METRICS_UPDATE = "connectors.status.update";

    @Inject
    ConnectorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    MetricsConfig metricsConfig;
    @Inject
    FleetShardSyncScheduler scheduler;

    @Inject
    @MetricsID(METRICS_SYNC)
    StaticMetricsRecorder syncRecorder;
    @Inject
    @MetricsID(METRICS_SYNC + ".total")
    Counter syncTotalRecorder;

    @Inject
    @MetricsID(METRICS_UPDATE)
    StaticMetricsRecorder updateRecorder;
    @Inject
    @MetricsID(METRICS_UPDATE + ".total")
    Counter updateTotalRecorder;

    private volatile Instant lastResync;
    private volatile Instant lastUpdate;

    private final ConcurrentMap<NamespacedName, Instant> connectors = new ConcurrentHashMap<>();

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting connector status sync");

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

    @Override
    public void stop() {
        scheduler.shutdownQuietly(JOB_ID);
    }

    public void run() {
        final Duration resyncInterval = config.resources().resyncInterval();
        final Instant now = Instant.now();
        final boolean resync = lastResync == null || greater(lastResync, now, resyncInterval);

        if (resync) {
            syncRecorder.record(this::sync);
            lastResync = now;
        } else {
            updateRecorder.record(this::update);
        }

        lastUpdate = now;
    }

    private void sync() {
        int count = 0;

        try {
            for (ManagedConnector connector : connectorClient.getAllConnectors()) {
                updater.update(connector);

                count++;
            }
        } finally {
            if (count > 0) {
                syncTotalRecorder.increment(count);
            }
        }
    }

    private void update() {
        int count = 0;

        try {
            for (Map.Entry<NamespacedName, Instant> entry : connectors.entrySet()) {
                if (entry.getValue().isAfter(lastUpdate)) {
                    connectorClient.getConnector(entry.getKey()).ifPresent(updater::update);

                    count++;
                }
            }
        } finally {
            if (count > 0) {
                updateTotalRecorder.increment(count);
            }
        }
    }

    private static boolean greater(Temporal startInclusive, Temporal endExclusive, Duration interval) {
        return Duration.between(startInclusive, endExclusive).compareTo(interval) >= 0;
    }
}
