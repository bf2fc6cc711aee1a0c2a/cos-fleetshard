package org.bf2.cos.fleetshard.operator.connector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentSync.class);

    @Inject
    FleetManagerClient fleetManager;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    ConnectorDeploymentProvisioner provisioner;
    @Inject
    FleetShardOperator operator;

    @Scheduled(
        every = "{cos.connectors.sync.all.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAllConnectorDeployments() {
        if (!operator.isRunning()) {
            return;
        }

        this.provisioner.submit(null, null);

        LOGGER.debug("Sync connectors status (queue_size={})", this.provisioner.size());
    }

    @Scheduled(
        every = "{cos.connectors.poll.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        if (!operator.isRunning()) {
            return;
        }

        LOGGER.debug("Poll connectors");

        fleetShard.lookupManagedConnectorCluster()
            .filter(cluster -> cluster.getStatus().isReady())
            .ifPresentOrElse(
                cluster -> {
                    LOGGER.debug("Polling for fleet manager connectors");

                    final String clusterId = cluster.getSpec().getId();
                    final String connectorsNamespace = cluster.getSpec().getConnectorsNamespace();

                    for (ConnectorDeployment deployment : fleetManager.getDeployments(clusterId, connectorsNamespace)) {
                        provisioner.submit(cluster, deployment);
                    }
                },
                () -> LOGGER.debug("Operator not yet configured"));
    }

    @Scheduled(
        every = "{cos.connectors.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runProvisioner() {

        if (!operator.isRunning()) {
            LOGGER.debug("Operator is not yet ready");
            return;
        }

        this.provisioner.run();
    }

}
