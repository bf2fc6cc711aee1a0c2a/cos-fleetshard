package org.bf2.cos.fleetshard.operator;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class FleetShardOperator {
    @Inject
    Operator operator;

    private AtomicBoolean running = new AtomicBoolean(false);

    void onStart(@Observes StartupEvent ignored) {
        if (running.compareAndSet(false, true)) {
            operator.start();
        }
    }

    void onStop(@Observes ShutdownEvent ignored) {
        if (running.compareAndSet(true, false)) {
            operator.close();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
