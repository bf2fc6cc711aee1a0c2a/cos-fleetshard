package org.bf2.cos.fleetshard.operator;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.config.InformerStoppedHandler;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.arc.Unremovable;

public class FleetShardProducers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardProducers.class);

    @Inject
    FleetShardOperatorConfig config;

    @SuppressWarnings("PMD.DoNotTerminateVM")
    @Singleton
    @Produces
    @Unremovable
    InformerStoppedHandler informerStoppedHandler() {
        return (informer, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Informer {} has stopped working, exiting", informer, throwable);

                System.exit(-1);
            }
        };
    }

    @Produces
    @Singleton
    @Unremovable
    public MeterFilter configureAllRegistries() {
        List<Tag> tags = new ArrayList<>();

        config.metrics().recorder().tags().common().forEach((k, v) -> {
            tags.add(Tag.of(k, v));
        });

        return MeterFilter.commonTags(tags);
    }
}
