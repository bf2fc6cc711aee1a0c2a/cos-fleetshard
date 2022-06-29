package org.bf2.cos.fleetshard.operator.service;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ServiceApplication {
    @Inject
    ServiceOperator operator;

    void onStart(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) StartupEvent ignored) {
        operator.start();
    }

    void onStop(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) ShutdownEvent ignored) {
        operator.stop();
    }
}
