package org.bf2.cos.fleetshard.sync.housekeeping;

import java.time.Instant;
import java.util.Collection;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

// This job removes deleted connectors metrics once a day. Note that it only removes the metrics that are 6 hours older
@DisallowConcurrentExecution
public class MetricsHousekeeperJob implements Job {

    public static final String CONNECTOR_STATE = "connector.state";
    public static final String CONNECTOR_STATE_COUNT = "connector.state.count";

    @Inject
    MeterRegistry registry;
    @Inject
    FleetShardSyncConfig config;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHousekeeperJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        LOGGER.info("Executing Metrics housekeeping");

        try {

            // Fetching counters with deletion_timestamp
            Collection<Counter> meters = registry.find(config.metrics().baseName() + "." + CONNECTOR_STATE_COUNT)
                .tagKeys("deletion_timestamp").counters();

            if (meters != null) {
                for (Counter meter : meters) {

                    String deletedAt = meter.getId().getTag("deletion_timestamp");
                    String id = meter.getId().getTag("cos.connector.id");

                    if (deletedAt != null && id != null && Instant.now()
                        .isAfter(Instant.parse(deletedAt).plus(config.resources().metricsHousekeeperDeleteMetricsAfter()))) {

                        LOGGER.info("Got connector id for deleting the metrics: {}", id);

                        Collection<Counter> counters = registry
                            .find(config.metrics().baseName() + "." + CONNECTOR_STATE_COUNT)
                            .tag("cos.connector.id", id).counters();
                        if (counters != null) {
                            // Removing the counter type metrics
                            for (Counter counter : counters) {
                                registry.remove(counter);
                            }
                        }

                        // removing the gauge type metrics
                        Gauge gauge = registry.find(config.metrics().baseName() + "." + CONNECTOR_STATE)
                            .tag("cos.connector.id", id).gauge();
                        if (gauge != null) {
                            registry.remove(gauge);
                        }

                        LOGGER.info("Deleted all connector metrics: {}", id);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Error while deleting old connectors metric", ex);

            // this job will refire immediately
            throw new JobExecutionException(ex);
        }

    }
}
