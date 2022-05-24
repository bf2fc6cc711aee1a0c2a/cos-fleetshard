package org.bf2.cos.fleetshard.sync.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Repeatable(MetricsTags.class)
@Documented
@Retention(RUNTIME)
public @interface MetricsTag {

    /**
     * The tags.
     *
     * @return the tags.
     */
    String key();

    /**
     * The name.
     *
     * @return the name.
     */
    String value();
}
