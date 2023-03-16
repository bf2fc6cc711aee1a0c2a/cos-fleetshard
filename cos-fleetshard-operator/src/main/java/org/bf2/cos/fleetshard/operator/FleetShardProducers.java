package org.bf2.cos.fleetshard.operator;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.config.InformerStoppedHandler;
import io.javaoperatorsdk.operator.api.monitoring.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.arc.Unremovable;

public class FleetShardProducers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardProducers.class);

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
    public MeterFilter configureAllRegistries(
        FleetShardOperatorConfig config,
        ManagedConnectorOperator managedConnectorOperator) {

        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("cos.operator.id", managedConnectorOperator.getMetadata().getName()));
        tags.add(Tag.of("cos.operator.type", managedConnectorOperator.getSpec().getType()));
        tags.add(Tag.of("cos.operator.version", managedConnectorOperator.getSpec().getVersion()));

        config.metrics().recorder().tags().common().forEach((k, v) -> {
            tags.add(Tag.of(k, v));
        });

        return MeterFilter.commonTags(tags);
    }

    @Produces
    @Singleton
    @Unremovable
    public Metrics getMetrics() {
        return Metrics.NOOP;
    }

    @Produces
    @Singleton
    @Unremovable
    public MeterBinder getMeterBinder() {
        return registry -> {
            // Do noting
        };
    }

}
