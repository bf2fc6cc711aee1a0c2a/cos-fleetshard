package org.bf2.cos.fleetshard.operator.connectoroperator;

import java.util.Objects;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.operator.it.support.WatcherEventSource;

public class ConnectorOperatorEventSource extends WatcherEventSource<ManagedConnectorOperator> {
    private final String connectorsNamespace;

    public ConnectorOperatorEventSource(KubernetesClient client, String connectorsNamespace) {
        super(client);

        this.connectorsNamespace = connectorsNamespace;
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

        var connectors = getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(this.connectorsNamespace)
            .list()
            .getItems();

        if (connectors == null) {
            return;
        }

        for (var connector : connectors) {
            if (connector.getStatus() == null) {
                continue;
            }
            if (connector.getStatus().getOperator() == null) {
                continue;
            }
            if (!Objects.equals(resource.getSpec().getType(), connector.getStatus().getOperator().getType())) {
                continue;
            }

            final var rv = new Version(resource.getSpec().getVersion());
            final var cv = new Version(connector.getStatus().getOperator().getVersion());

            if (rv.compareTo(cv) > 0) {
                getLogger().info("ManagedConnectorOperator updated: {}", resource.getSpec());

                eventHandler.handleEvent(new AbstractEvent(connector.getMetadata().getUid(), this) {
                });
            }
        }
    }
}
