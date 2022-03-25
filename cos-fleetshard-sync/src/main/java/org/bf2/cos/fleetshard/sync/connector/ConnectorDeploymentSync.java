package org.bf2.cos.fleetshard.sync.connector;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Retry;
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
    ConnectorNamespaceProvisioner namespaceProvisioner;
    @Inject
    ManagedExecutor executor;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetManagerClient fleetManager;
    @Inject
    FleetShardClient fleetShardClient;

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
            final long timeout = config.connectors().provisioner().queueTimeout().toMillis();

            while (!executor.isShutdown()) {
                provision(timeout);
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

    @Retry(maxRetries = 10, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public void provision(long timeout) throws InterruptedException {
        if (config.tenancy().enabled()) {
            // TODO: should not be executed each time
            fleetManager.getNamespaces(0, this::provisionNamespaces);
        }

        queue.poll(timeout, TimeUnit.MILLISECONDS, this::provisionConnectors);
    }

    private void provisionNamespaces(Collection<ConnectorNamespace> namespaces) {
        LOGGER.debug("namespaces: {}", namespaces.size());

        for (ConnectorNamespace namespace : namespaces) {
            namespaceProvisioner.provision(namespace);
        }
    }

    private void provisionConnectors(Collection<ConnectorDeployment> deployments) {
        LOGGER.debug("deployments: {}", deployments.size());

        for (ConnectorDeployment deployment : deployments) {
            provisioner.provision(deployment);
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
