package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.EventSource;
import org.bf2.cos.fleetshard.api.connector.Connector;

public class ConnectorEvent<T extends Connector<?, ?>> extends DependantResourceEvent<T> {
    public ConnectorEvent(
            Watcher.Action action,
            T resource,
            String ownerUid,
            EventSource eventSource) {

        super(action, resource, ownerUid, eventSource);
    }
}