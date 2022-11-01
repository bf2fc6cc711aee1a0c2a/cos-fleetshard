package org.bf2.cos.fleetshard.sync.it.support;

import org.bf2.cos.fleetshard.support.metrics.MetricsConfig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

public final class MetricsSupport {
    private MetricsSupport() {
    }

    public static Search find(MeterRegistry registry, MetricsConfig config, String... subs) {
        return find(registry, config.baseName(), subs);
    }

    public static Search find(MeterRegistry registry, String base, String... subs) {
        String id = base;

        if (subs.length > 0) {
            id += "." + String.join(".", subs);
            id = id.replace("..", ".");
        }

        return registry.find(id);
    }

    public static Counter counter(MeterRegistry registry, MetricsConfig config, String... subs) {
        return find(registry, config, subs).counter();
    }

    public static Counter counter(MeterRegistry registry, String base, String... subs) {
        return find(registry, base, subs).counter();
    }
}
