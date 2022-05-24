package org.bf2.cos.fleetshard.sync.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
public @interface MetricsTags { MetricsTag[] value(); }
