package org.bf2.cos.fleetshard.operator.camel;

import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.operator.support.ConnectorEventSource;

import io.fabric8.kubernetes.client.KubernetesClient;

public class CamelConnectorEventSource extends ConnectorEventSource<CamelConnector> {
    public static String EVENT_SOURCE_ID = "camel-connector-event-source";

    public CamelConnectorEventSource(KubernetesClient client) {
        super(client);
    }

    @Override
    protected void watch() {
        getClient().customResources(CamelConnector.class).watch(this);
    }

    @Override
    public void eventReceived(Action action, CamelConnector resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        eventHandler.handleEvent(
                new CamelConnectorEvent(
                        action,
                        resource,
                        "",
                        this));
    }
}
