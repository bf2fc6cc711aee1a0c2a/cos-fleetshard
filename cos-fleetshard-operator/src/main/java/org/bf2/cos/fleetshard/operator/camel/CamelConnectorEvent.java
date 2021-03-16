package org.bf2.cos.fleetshard.operator.camel;

import io.fabric8.kubernetes.client.Watcher;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;

public class CamelConnectorEvent extends DependantResourceEvent {
    public CamelConnectorEvent(
            Watcher.Action action,
            CamelConnector resource,
            CamelConnectorEventSource eventSource) {
        super(action, resource, eventSource);
    }

    @Override
    public String toString() {
        return "CamelConnectorEvent{}";
    }
}
