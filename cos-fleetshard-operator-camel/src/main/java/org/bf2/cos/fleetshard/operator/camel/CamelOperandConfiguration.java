package org.bf2.cos.fleetshard.operator.camel;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.camel")
public interface CamelOperandConfiguration {
    LabelSelection labelSelection();

    RouteController routeController();

    Health health();

    ExchangePooling exchangePooling();

    Connectors connectors();

    interface LabelSelection {
        @WithDefault("true")
        boolean enabled();
    }

    interface RouteController {
        @WithDefault("10s")
        String backoffDelay();

        @WithDefault("0s")
        String initialDelay();

        @WithDefault("1")
        String backoffMultiplier();

        @WithDefault("6")
        String backoffMaxAttempts();
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

    interface ExchangePooling {
        // The pooled exchange factory strategy has side effects on some components:
        // - https://issues.redhat.com/browse/MGDCTRS-1058
        // - https://issues.redhat.com/browse/MGDCTRS-1057
        // - https://issues.apache.org/jira/browse/CAMEL-18187
        //
        // Default changed to "prototype" till we get a fix in Apache Camel
        @WithDefault("prototype")
        String exchangeFactory();

        @WithDefault("100")
        String exchangeFactoryCapacity();

        @WithDefault("false")
        String exchangeFactoryStatisticsEnabled();
    }

    interface ConnectorConfiguration {
        Map<String, String> traits();
    }

    interface Connectors extends ConnectorConfiguration {
        Map<String, ConnectorConfiguration> types();

        Processors processors();
    }

    interface Processors {
        @WithDefault("false")
        boolean enabled();
    }
}