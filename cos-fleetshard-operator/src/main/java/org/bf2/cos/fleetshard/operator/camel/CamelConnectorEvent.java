package org.bf2.cos.fleetshard.operator.camel;

import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.operator.support.ConnectorEvent;

import io.fabric8.kubernetes.client.Watcher;

public class CamelConnectorEvent extends ConnectorEvent<CamelConnector> {
    public CamelConnectorEvent(
            Watcher.Action action,
            CamelConnector resource,
            String ownerUid,
            CamelConnectorEventSource eventSource) {
        super(action, resource, ownerUid, eventSource);
    }

    @Override
    public String toString() {
        return "KameletBindingEvent{}";
    }
}
