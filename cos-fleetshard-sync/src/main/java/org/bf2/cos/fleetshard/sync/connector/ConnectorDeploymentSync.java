package org.bf2.cos.fleetshard.sync.connector;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentSync.class);

    AutoCloseable operatorsObserver;

    @Inject
    ConnectorDeploymentQueue queue;
    @Inject
    ConnectorDeploymentProvisioner provisioner;
    @Inject
    ManagedExecutor executor;
    @Inject
    FleetShardClient connectorClient;

    @ConfigProperty(name = "cos.connectors.provisioner.queue.timeout", defaultValue = "15s")
    Duration timeout;
    @ConfigProperty(name = "cos.operators.observe", defaultValue = "true")
    boolean observeOperators;

    private volatile Future<?> future;

    public void start() {
        if (!timeout.isZero()) {
            LOGGER.info("Starting deployment sync");
            future = executor.submit(this::run);
        }

        if (observeOperators) {
            LOGGER.info("Starting operators observer");
            operatorsObserver = connectorClient.watchAllOperators(operator -> queue.submitPoisonPill());
        }
    }

    public void stop() {
        if (this.operatorsObserver != null) {
            try {
                this.operatorsObserver.close();
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
                final Collection<ConnectorDeployment> deployments = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
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
