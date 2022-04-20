package org.bf2.cos.fleetshard.sync.housekeeping;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.All;

@ApplicationScoped
public class Housekeeper implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(Housekeeper.class);

    private static final String JOB_ID = "cos.resources.housekeeping";
    public static final String METRICS_ID = "housekeeping";

    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    MeterRegistry registry;
    @Inject
    @All
    List<Task> tasks;

    private volatile MetricsRecorder recorder;

    @Override
    public void start() throws Exception {
        if (config.resources().housekeeperInterval().isZero()) {
            LOGGER.info("Skipping starting house keeper as interval is zero");
            return;
        }

        for (Task task : tasks) {
            if (task instanceof Service) {
                ((Service) task).start();
            }
        }

        recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + METRICS_ID);

        scheduler.schedule(
            JOB_ID,
            HousekeeperJob.class,
            config.resources().housekeeperInterval());
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownQuietly(JOB_ID);

        for (Task task : tasks) {
            if (task instanceof Service) {
                ((Service) task).stop();
            }
        }
    }

    public void run() {
        for (Task task : tasks) {
            recorder.record(task, task.getId());
        }
    }

    public interface Task extends Runnable {
        String getId();
    }
}
