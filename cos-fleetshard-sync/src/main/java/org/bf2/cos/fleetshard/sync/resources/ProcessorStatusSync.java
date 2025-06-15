package org.bf2.cos.fleetshard.sync.resources;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.metrics.StaticMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.metrics.MetricsID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.micrometer.core.instrument.Counter;

@ApplicationScoped
public class ProcessorStatusSync implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorStatusSync.class);

    public static final String JOB_ID = "cos.processors.status.sync";
    public static final String METRICS_SYNC = "processors.status.sync";
    public static final String METRICS_UPDATE = "processors.status.update";

    @Inject
    ProcessorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    FleetShardSyncConfig config;
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

    private final ConcurrentMap<NamespacedName, Instant> processors = new ConcurrentHashMap<>();

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting processor status sync");

        connectorClient.watchProcessors(new ResourceEventHandler<>() {
            @Override
            public void onAdd(ManagedProcessor connector) {
                processors.put(NamespacedName.of(connector), Instant.now());
            }

            @Override
            public void onUpdate(ManagedProcessor ignored, ManagedProcessor connector) {
                processors.put(NamespacedName.of(connector), Instant.now());
            }

            @Override
            public void onDelete(ManagedProcessor connector, boolean deletedFinalStateUnknown) {
                processors.remove(NamespacedName.of(connector));
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
            for (ManagedProcessor processor : connectorClient.getAllProcessors()) {
                updater.update(processor);

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
            for (Map.Entry<NamespacedName, Instant> entry : processors.entrySet()) {
                if (entry.getValue().isAfter(lastUpdate)) {
                    connectorClient.getProcessor(entry.getKey()).ifPresent(updater::update);

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
