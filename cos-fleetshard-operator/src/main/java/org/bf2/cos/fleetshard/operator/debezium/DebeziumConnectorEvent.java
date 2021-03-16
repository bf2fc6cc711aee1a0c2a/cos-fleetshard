package org.bf2.cos.fleetshard.operator.debezium;

import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;

import io.fabric8.kubernetes.client.Watcher;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;

public class DebeziumConnectorEvent extends DependantResourceEvent {
    public DebeziumConnectorEvent(
            Watcher.Action action,
            DebeziumConnector resource,
            DebeziumConnectorEventSource eventSource) {
        super(action, resource, eventSource);
    }

    @Override
    public String toString() {
        return "DebeziumConnectorEvent{}";
    }
}
