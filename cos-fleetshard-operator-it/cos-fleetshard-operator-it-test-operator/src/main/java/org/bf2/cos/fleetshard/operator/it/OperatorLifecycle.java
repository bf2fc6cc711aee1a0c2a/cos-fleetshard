package org.bf2.cos.fleetshard.operator.it;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class OperatorLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorLifecycle.class);

    @Inject
    Operator operator;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Starting the operator");
        operator.start();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Stopping the operator");
        operator.close();
    }
}
