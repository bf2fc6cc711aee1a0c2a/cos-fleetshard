package org.bf2.cos.fleetshard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Application {
    @Inject
    Operator operator;

    void onStart(@Observes StartupEvent ignored) {
        operator.start();
    }

    void onStop(@Observes ShutdownEvent ignored) {
        operator.close();
    }
}
