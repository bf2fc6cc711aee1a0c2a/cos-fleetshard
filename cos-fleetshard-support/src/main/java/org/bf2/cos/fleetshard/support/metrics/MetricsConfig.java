package org.bf2.cos.fleetshard.support.metrics;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "cos.metrics")
public interface MetricsConfig {
    String baseName();
}
