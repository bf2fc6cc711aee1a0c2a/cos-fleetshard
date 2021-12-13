package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStatusSync.class);

    @Inject
    ConnectorStatusUpdater updater;
    @Inject
    FleetShardClient connectorClient;
    @Inject
    ConnectorStatusQueue queue;
    @Inject
    ManagedExecutor executor;
    @Inject
    FleetShardSyncConfig config;

    private volatile Future<?> future;

    public void start() {
        LOGGER.info("Starting connector status sync");
        future = executor.submit(this::run);

        if (config.connectors().watch()) {
            LOGGER.info("Starting connector status observer");
            connectorClient.watchConnectors(
                connector -> queue.submit(connector.getMetadata().getName()));
        }
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }

    private void run() {
        try {
            while (!executor.isShutdown()) {
                final long timeout = config.connectors().status().queueTimeout().toMillis();
                final Collection<ManagedConnector> connectors = queue.poll(timeout, TimeUnit.MILLISECONDS);
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
