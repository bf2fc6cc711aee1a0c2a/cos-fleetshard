package org.bf2.cos.fleetshard.sync;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class FleetShardApplication {
    @Inject
    FleetShardSync sync;

    void onStart(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) StartupEvent ignored) {
        sync.start();
    }

    void onStop(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) ShutdownEvent ignored) {
        sync.stop();
    }
}
