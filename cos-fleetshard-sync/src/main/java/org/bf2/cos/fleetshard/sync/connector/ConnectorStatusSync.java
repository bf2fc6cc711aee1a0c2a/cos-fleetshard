package org.bf2.cos.fleetshard.sync.connector;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ConnectorStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusSync.class);

    AutoCloseable observer;

    @Inject
    ConnectorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    ConnectorStatusQueue queue;
    @Inject
    ManagedExecutor executor;

    @ConfigProperty(name = "cos.connectors.status.queue.timeout", defaultValue = "15s")
    Duration timeout;
    @ConfigProperty(name = "cos.connectors.status.sync.observe", defaultValue = "true")
    boolean observe;

    private volatile Future<?> future;

    void onStart(
        @Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent ignored) {
        LOGGER.info("Starting connector status sync");
        future = executor.submit(this::run);

        if (observe) {
            LOGGER.info("Starting connector status observer");
            observer = connectorClient.watchAllConnectors(connector -> queue.submit(connector.getMetadata().getName()));
        }
    }

    void onStop(
        @Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent ignored) {
        if (this.observer != null) {
            try {
                this.observer.close();
            } catch (Exception e) {
                LOGGER.debug("", e);
            }
        }
        if (future != null) {
            future.cancel(true);
        }
    }

    private void run() {
        try {
            while (!executor.isShutdown()) {
                final Collection<ManagedConnector> connectors = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                LOGGER.debug("connectors to update: {}", connectors.size());

                for (ManagedConnector connector : connectors) {
                    updater.update(connector);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("interrupted, message:{}", e.getMessage());
        } finally {
            if (!executor.isShutdown()) {
                future = executor.submit(this::run);
            }
        }
    }
}
