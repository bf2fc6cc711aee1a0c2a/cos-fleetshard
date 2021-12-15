package org.bf2.cos.fleetshard.sync.connector;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentSync.class);

    @Inject
    ConnectorDeploymentQueue queue;
    @Inject
    ConnectorDeploymentProvisioner provisioner;
    @Inject
    ManagedExecutor executor;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;

    private volatile Future<?> future;

    public void start() throws Exception {
        scheduler.schedule(
            PollJob.ID,
            PollJob.class,
            config.connectors().pollInterval());
        scheduler.schedule(
            ReSynkJob.ID,
            ReSynkJob.class,
            config.connectors().resyncInterval());

        if (!config.connectors().provisioner().queueTimeout().isZero()) {
            LOGGER.info("Starting deployment sync");
            future = executor.submit(this::run);
        }
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
        }

        try {
            scheduler.shutdown(PollJob.ID);
        } catch (Exception ignored) {
        }

        try {
            scheduler.shutdown(ReSynkJob.ID);
        } catch (Exception ignored) {
        }
    }

    private void run() {
        try {
            while (!executor.isShutdown()) {
                final long timeout = config.connectors().provisioner().queueTimeout().toMillis();
                queue.poll(timeout, TimeUnit.MILLISECONDS, deployments -> {
                    LOGGER.debug("connectors to deploy: {}", deployments.size());

                    for (ConnectorDeployment deployment : deployments) {
                        provisioner.provision(deployment);
                    }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("interrupted, message: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.debug("{}", e.getMessage(), e);
        } finally {
            if (!executor.isShutdown()) {
                future = executor.submit(this::run);
            }
        }
    }

    @DisallowConcurrentExecution
    public static class PollJob implements Job {
        public static final String ID = "cos.connectors.poll";

        @Inject
        ConnectorDeploymentQueue queue;

        @Inject
        FleetShardClient connectorClient;

        @MetricsID(ID)
        @Inject
        MetricsRecorder recorder;

        @Override
        public void execute(JobExecutionContext context) {
            recorder.record(() -> this.queue.submit(connectorClient.getMaxDeploymentResourceRevision()));
        }
    }

    @DisallowConcurrentExecution
    public static class ReSynkJob implements Job {
        public static final String ID = "cos.connectors.resync";

        @Inject
        ConnectorDeploymentQueue queue;

        @MetricsID(ID)
        @Inject
        MetricsRecorder recorder;

        @Override
        public void execute(JobExecutionContext context) {
            recorder.record(queue::submitPoisonPill);
        }
    }

}
