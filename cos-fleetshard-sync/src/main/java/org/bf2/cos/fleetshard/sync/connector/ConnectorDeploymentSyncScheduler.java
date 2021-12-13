package org.bf2.cos.fleetshard.sync.connector;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class ConnectorDeploymentSyncScheduler {
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetShardSyncConfig config;

    @PostConstruct
    void init() throws SchedulerException {
        scheduler.schedule(
            PollJob.ID,
            PollJob.class,
            config.connectors().pollInterval());
        scheduler.schedule(
            ReSynkJob.ID,
            ReSynkJob.class,
            config.connectors().resyncInterval());
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
