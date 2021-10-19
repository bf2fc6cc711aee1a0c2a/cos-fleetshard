package org.bf2.cos.fleetshard.support.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
public @interface MetricsID {

    /**
     * The name.
     *
     * @return the name.
     */
    String value() default "";
}
