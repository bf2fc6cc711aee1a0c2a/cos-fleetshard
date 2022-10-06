package org.bf2.cos.fleetshard.support.coordination;

import java.time.Duration;

import org.bf2.cos.fleetshard.support.DurationConverter;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.lease")
public interface LeaseConfig {
    @WithDefault("false")
    boolean enabled();

    String namespace();

    String name();

    String id();

    @WithDefault("15s")
    @WithConverter(DurationConverter.class)
    Duration duration();

    @WithDefault("10s")
    @WithConverter(DurationConverter.class)
    Duration renewalDeadline();

    @WithDefault("2s")
    @WithConverter(DurationConverter.class)
    Duration retryPeriod();
}
