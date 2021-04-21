package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;

import io.fabric8.kubernetes.client.Watcher;

public class ConnectorEvent extends DependantResourceEvent {
    public ConnectorEvent(
            Watcher.Action action,
            ManagedConnector resource,
            ConnectorEventSource eventSource) {
        super(action, resource, eventSource);
    }
}
