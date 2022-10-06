package org.bf2.cos.fleetshard.operator;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.config.InformerStoppedHandler;
import io.quarkus.arc.Unremovable;

public class FleetShardProducers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardProducers.class);

    @SuppressWarnings("PMD.DoNotTerminateVM")
    @Singleton
    @Produces
    @Unremovable
    InformerStoppedHandler informerStoppedHandler() {
        return (informer, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Informer {} has stopped working, exiting", informer, throwable);

                System.exit(-1);
            }
        };
    }
}
