package org.bf2.cos.fleetshard.operator.camel;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.camel")
public interface CamelOperandConfiguration {
    RouteController routeController();

    Health health();

    interface RouteController {
        @WithDefault("10s")
        String backoffDelay();

        @WithDefault("0s")
        String initialDelay();

        @WithDefault("1")
        String backoffMultiplier();
    }

    interface Health {
        @WithDefault("1")
        String livenessSuccessThreshold();

        @WithDefault("3")
        String livenessFailureThreshold();

        @WithDefault("10")
        String livenessPeriodSeconds();

        @WithDefault("1")
        String livenessTimeoutSeconds();

        @WithDefault("1")
        String readinessSuccessThreshold();

        @WithDefault("3")
        String readinessFailureThreshold();

        @WithDefault("10")
        String readinessPeriodSeconds();

        @WithDefault("1")
        String readinessTimeoutSeconds();
    }
}