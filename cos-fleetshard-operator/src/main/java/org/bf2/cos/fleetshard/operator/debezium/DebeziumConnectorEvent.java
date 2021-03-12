package org.bf2.cos.fleetshard.operator.debezium;

import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.operator.support.ConnectorEvent;

import io.fabric8.kubernetes.client.Watcher;

public class DebeziumConnectorEvent extends ConnectorEvent<DebeziumConnector> {
    public DebeziumConnectorEvent(
            Watcher.Action action,
            DebeziumConnector resource,
            String ownerUid,
            DebeziumConnectorEventSource eventSource) {
        super(action, resource, ownerUid, eventSource);
    }

    @Override
    public String toString() {
        return "KameletBindingEvent{}";
    }
}
