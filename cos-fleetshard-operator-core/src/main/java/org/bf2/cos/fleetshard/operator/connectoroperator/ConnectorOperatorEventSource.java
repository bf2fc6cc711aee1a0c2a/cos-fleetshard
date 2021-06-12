package org.bf2.cos.fleetshard.operator.connectoroperator;

import java.util.Objects;

import io.fabric8.kubernetes.client.Watch;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.operator.fleet.FleetShardClient;
import org.bf2.cos.fleetshard.operator.it.support.WatcherEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorOperatorEventSource extends WatcherEventSource<ManagedConnectorOperator> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorOperatorEventSource.class);

    private final FleetShardClient fleetShardClient;

    public ConnectorOperatorEventSource(FleetShardClient fleetShardClient) {
        super(fleetShardClient.getKubernetesClient());

        this.fleetShardClient = fleetShardClient;
    }

    @Override
    protected Watch watch() {
        return getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(getClient().getNamespace())
            .watch(this);
    }

    @Override
    public void eventReceived(Action action, ManagedConnectorOperator resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        LOGGER.info("Event {} received on operator: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        for (var connector : fleetShardClient.lookupConnectors()) {
            if (connector.getStatus() == null) {
                continue;
            }
            if (connector.getStatus().getAssignedOperator() == null) {
                continue;
            }
            if (!Objects.equals(resource.getSpec().getType(), connector.getStatus().getAssignedOperator().getType())) {
                continue;
            }

            final var rv = new Version(resource.getSpec().getVersion());
            final var cv = new Version(connector.getStatus().getAssignedOperator().getVersion());

            if (rv.compareTo(cv) > 0) {
                getLogger().info("ManagedConnectorOperator updated, connector: {}/{}, operator: {}",
                    connector.getMetadata().getNamespace(),
                    connector.getMetadata().getName(),
                    resource.getSpec());

                eventHandler.handleEvent(new ConnectorOperatorEvent(connector.getMetadata().getUid(), this));
            }
        }
    }
}
