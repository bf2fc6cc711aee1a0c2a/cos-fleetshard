package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEventSource;

import io.fabric8.kubernetes.client.KubernetesClient;

public class ConnectorEventSource extends DependantResourceEventSource<Connector> {
    public static String EVENT_SOURCE_ID = "connector-event-source";

    public ConnectorEventSource(KubernetesClient client) {
        super(client);
    }

    @Override
    protected void watch() {
        //TODO: filter namespace ?
        //TODO: filter labels ?
        getClient().customResources(Connector.class).watch(this);
    }

    @Override
    public void eventReceived(Action action, Connector resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        eventHandler.handleEvent(
                new ConnectorEvent(
                        action,
                        resource,
                        this));
    }
}
