package org.bf2.cos.fleetshard.sync.connector;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class ConnectorStatusSyncScheduler {
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetShardSyncConfig config;

    @PostConstruct
    void init() throws SchedulerException {
        scheduler.schedule(
            ReSynkJob.ID,
            ReSynkJob.class,
            config.connectors().status().resyncInterval());
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
}
