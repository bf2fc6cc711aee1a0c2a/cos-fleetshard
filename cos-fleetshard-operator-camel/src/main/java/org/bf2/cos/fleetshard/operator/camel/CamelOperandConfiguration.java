package org.bf2.cos.fleetshard.operator.camel;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.camel")
public interface CamelOperandConfiguration {
    RouteController routeController();

    interface RouteController {
        @WithDefault("10s")
        String backoffDelay();

        @WithDefault("0s")
        String initialDelay();

        @WithDefault("1")
        String backoffMultiplier();
    }
}