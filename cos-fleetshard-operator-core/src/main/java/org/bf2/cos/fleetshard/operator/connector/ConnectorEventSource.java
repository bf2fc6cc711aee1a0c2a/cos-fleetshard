package org.bf2.cos.fleetshard.operator.connector;

import io.fabric8.kubernetes.client.Watch;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.fleet.FleetShardClient;
import org.bf2.cos.fleetshard.operator.it.support.WatcherEventSource;

public class ConnectorEventSource extends WatcherEventSource<ManagedConnector> {
    private final FleetShardClient fleetShardClient;

    public ConnectorEventSource(FleetShardClient fleetShardClient) {
        super(fleetShardClient.getKubernetesClient());

        this.fleetShardClient = fleetShardClient;
    }

    @Override
    protected Watch watch() {
        return getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(fleetShardClient.getConnectorsNamespace())
            .watch(this);
    }

    @Override
    public void eventReceived(Action action, ManagedConnector connector) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        getLogger().info("Event {} received on connector: {}/{}",
            action.name(),
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());

        eventHandler.handleEvent(new ConnectorEvent(
            connector.getMetadata().getOwnerReferences().get(0).getUid(),
            this,
            connector.getMetadata().getName(),
            connector.getMetadata().getNamespace()));
    }
}
