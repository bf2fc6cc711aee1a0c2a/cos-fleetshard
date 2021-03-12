package org.bf2.cos.fleetshard.operator.debezium;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEventSource;

public class DebeziumConnectorEventSource extends DependantResourceEventSource<DebeziumConnector> {
    public static String EVENT_SOURCE_ID = "debezium-connector-event-source";

    public DebeziumConnectorEventSource(KubernetesClient client) {
        super(client);
    }

    @Override
    protected void watch() {
        getClient().customResources(DebeziumConnector.class).watch(this);
    }

    @Override
    public void eventReceived(Action action, DebeziumConnector resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        eventHandler.handleEvent(
                new DebeziumConnectorEvent(
                        action,
                        resource,
                        "",
                        this));
    }
}
