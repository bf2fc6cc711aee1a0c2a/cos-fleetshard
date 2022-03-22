package org.bf2.cos.fleetshard.sync.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusSync.class);

    @Inject
    ConnectorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    ConnectorStatusQueue queue;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;

    public void start() throws Exception {
        LOGGER.info("Starting connector status sync");

        scheduler.schedule(
            ReSynkJob.ID,
            ReSynkJob.class,
            config.connectors().status().resyncInterval());
        scheduler.schedule(
            UpdateJob.ID,
            UpdateJob.class,
            config.connectors().status().updateInterval());

        if (config.connectors().watch()) {
            LOGGER.info("Starting connector status observer");
            connectorClient.watchConnectors(connector -> queue.submit(NamespacedName.of(connector)));
        }
    }

    public void stop() {
        try {
            scheduler.shutdown(ReSynkJob.ID);
        } catch (Exception ignored) {
        }

        try {
            scheduler.shutdown(UpdateJob.ID);
        } catch (Exception ignored) {
        }
    }

    public void run() {
        queue.run(connectors -> {
            LOGGER.debug("connectors to update: {}", connectors.size());

            for (ManagedConnector connector : connectors) {
                updater.update(connector);
            }
        });
    }

    @DisallowConcurrentExecution
    public static class ReSynkJob implements Job {
        public static final String ID = "cos.connectors.status.resync";

        @Inject
        ConnectorStatusQueue queue;

        @MetricsID(ID)
        @Inject
        MetricsRecorder recorder;

        @Override
        public void execute(JobExecutionContext context) {
            recorder.record(queue::submitPoisonPill);
        }
    }

    @DisallowConcurrentExecution
    public static class UpdateJob implements Job {
        public static final String ID = "cos.connectors.status.update";

        @Inject
        ConnectorStatusSync sync;

        @MetricsID(ID)
        @Inject
        MetricsRecorder recorder;

        @Override
        public void execute(JobExecutionContext context) {
            recorder.record(sync::run);
        }
    }
}
