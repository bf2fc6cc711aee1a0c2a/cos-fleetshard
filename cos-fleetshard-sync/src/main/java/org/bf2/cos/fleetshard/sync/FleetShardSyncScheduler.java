package org.bf2.cos.fleetshard.sync;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FleetShardSyncScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardSyncScheduler.class);

    @Inject
    org.quartz.Scheduler quartz;

    public void schedule(String id, Class<? extends Job> jobType, Duration interval) throws SchedulerException {
        if (interval.isZero()) {
            LOGGER.debug("Skipping scheduling job of type {} with id {} as the duration is zero", id, jobType);
            return;
        }

        final JobDetail job = JobBuilder.newJob(jobType)
            .withIdentity(id + ".job", id)
            .build();

        final Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(id + ".trigger", id)
            .startNow()
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(interval.toMillis())
                .repeatForever())
            .build();

        quartz.scheduleJob(
            job,
            trigger);
    }

    public void shutdown(String id) throws SchedulerException {
        quartz.deleteJob(JobKey.jobKey(id + ".job", id));
    }

    public void shutdownQuietly(String id) {
        try {
            shutdown(id);
        } catch (SchedulerException e) {
            LOGGER.debug("Error deleting job {}", id, e);
        }
    }
}
