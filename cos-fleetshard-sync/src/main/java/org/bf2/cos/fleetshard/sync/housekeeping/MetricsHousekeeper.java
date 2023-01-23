package org.bf2.cos.fleetshard.sync.housekeeping;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetricsHousekeeper implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHousekeeper.class);

    private static final String JOB_ID = "cos.resources.metric.housekeeping";

    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;

    @Override
    public void start() throws Exception {
        if (config.resources().metricsHousekeeperInterval().isZero()) {
            LOGGER.info("Skipping starting house keeper as interval is zero");
            return;
        }

        scheduler.schedule(
            JOB_ID,
            MetricsHousekeeperJob.class,
            config.resources().metricsHousekeeperInterval());
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownQuietly(JOB_ID);
    }

}
