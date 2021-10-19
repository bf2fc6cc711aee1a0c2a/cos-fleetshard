package org.bf2.cos.fleetshard.support.metrics;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;

@Singleton
public class MetricsProducers {
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

        return MetricsRecorder.of(registry, named.value());
    }
}
