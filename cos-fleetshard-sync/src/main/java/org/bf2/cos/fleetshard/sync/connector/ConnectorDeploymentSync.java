package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentSync.class);

    @Inject
    ConnectorDeploymentQueue queue;
    @Inject
    ConnectorDeploymentProvisioner provisioner;
    @Inject
    ManagedExecutor executor;
    @Inject
    FleetShardSyncConfig config;

    private volatile Future<?> future;

    public void start() {
        if (!config.connectors().provisioner().queueTimeout().isZero()) {
            LOGGER.info("Starting deployment sync");
            future = executor.submit(this::run);
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
                final long timeout = config.connectors().provisioner().queueTimeout().toMillis();
                final Collection<ConnectorDeployment> deployments = queue.poll(timeout, TimeUnit.MILLISECONDS);
                LOGGER.debug("connectors to deploy: {}", deployments.size());

                for (ConnectorDeployment deployment : deployments) {
                    provisioner.provision(deployment);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("interrupted, message: {}", e.getMessage());
        } finally {
            if (!executor.isShutdown()) {
                future = executor.submit(this::run);
            }
        }
    }

}
