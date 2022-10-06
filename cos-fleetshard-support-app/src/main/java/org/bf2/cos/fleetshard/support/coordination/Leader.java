package org.bf2.cos.fleetshard.support.coordination;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.bf2.cos.fleetshard.support.Application;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfigBuilder;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElector;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Leader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Leader.class);

    private LeaderElector<?> elector;

    @Inject
    LeaseConfig config;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Application application;
    @Inject
    ManagedExecutor executor;
    @Inject
    LeaseLock lock;

    @PostConstruct
    void setUp() {
        if (config.enabled()) {
            elector = kubernetesClient.leaderElector()
                .withConfig(
                    new LeaderElectionConfigBuilder()
                        .withName(config.name())
                        .withLeaseDuration(config.duration())
                        .withRenewDeadline(config.renewalDeadline())
                        .withRetryPeriod(config.retryPeriod())
                        .withLock(lock)
                        .withLeaderCallbacks(new LeaderCallbacks(
                            () -> {
                                try {
                                    LOGGER.info("Start leading {}/{}", config.name(), config.id());

                                    application.start();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> {
                                try {
                                    LOGGER.info("Stop leading {}/{}", config.name(), config.id());

                                    application.stop();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            newLeader -> LOGGER.info("New leader elected {}", newLeader)))
                        .build())
                .build();
        }
    }

    void onStart(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) StartupEvent ignored) throws Exception {

        if (elector != null) {
            LOGGER.info("Start leader elector for {}/{}", config.name(), config.id());
            executor.runAsync(() -> elector.run());
            LOGGER.info("Leader elector for {}/{} started", config.name(), config.id());
        } else {
            LOGGER.info("Staring app {}/{}", config.name(), config.id());
            application.start();
            LOGGER.info("App {}/{} started", config.name(), config.id());
        }
    }

    void onStop(
        @Observes @Priority(Interceptor.Priority.APPLICATION + 10) ShutdownEvent ignored) throws Exception {
        application.stop();
    }
}