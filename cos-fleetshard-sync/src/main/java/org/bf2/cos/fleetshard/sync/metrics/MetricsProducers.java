package org.bf2.cos.fleetshard.sync.metrics;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@Singleton
public class MetricsProducers {
    @Inject
    FleetShardSyncConfig config;
    @Inject
    MeterRegistry registry;

    @Produces
    public MetricsRecorder recorder(InjectionPoint ip) {
        MetricsID named = ip.getAnnotated().getAnnotation(MetricsID.class);
        if (named == null) {
            throw new IllegalArgumentException("Missing MetricsID annotation");
        }
        if (named.value() == null || named.value().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing metrics id");
        }

        MetricsTags tags = ip.getAnnotated().getAnnotation(MetricsTags.class);
        if (tags != null && tags.value() == null) {
            throw new IllegalArgumentException("Missing metrics tags");
        }

        String id = named.value();
        if (!id.startsWith(config.metrics().baseName() + ".")) {
            id = config.metrics().baseName() + "." + id;
        }

        return MetricsRecorder.of(
            registry,
            id,
            tags == null
                ? Collections.emptyList()
                : Stream.of(tags.value()).map(t -> Tag.of(t.key(), t.value())).collect(Collectors.toList()));
    }
}
