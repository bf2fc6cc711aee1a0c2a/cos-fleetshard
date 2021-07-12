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
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    ConnectorDeploymentProvisioner provision;
    @Inject
    FleetShardOperator operator;

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
                    LOGGER.debug("Polling for control plane connectors");

                    final String clusterId = cluster.getSpec().getId();
                    final String connectorsNamespace = cluster.getSpec().getConnectorsNamespace();

                    for (ConnectorDeployment deployment : controlPlane.getDeployments(clusterId, connectorsNamespace)) {
                        provision.provision(cluster, deployment);
                    }
                },
                () -> LOGGER.debug("Operator not yet configured"));
    }
}
