package org.bf2.cos.fleetshard.operator.connector;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;

@ApplicationScoped
public class ConnectorControllerScheduler {
    @Inject
    FleetShardClient fleetShard;
    @Inject
    ConnectorController controller;

    @Scheduled(every = "{cos.connectors.resync.interval:60s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void resync() {
        List<ManagedConnector> connectors = fleetShard.lookupManagedConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            ManagedConnector mc = connectors.get(i);
            if (mc.getStatus() != null && mc.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor)) {
                controller.refresher(mc.getMetadata().getUid());
            }
        }
    }
}
