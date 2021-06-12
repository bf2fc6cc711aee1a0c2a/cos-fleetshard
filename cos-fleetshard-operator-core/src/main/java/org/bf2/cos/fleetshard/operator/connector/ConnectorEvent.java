package org.bf2.cos.fleetshard.operator.connector;

import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import io.javaoperatorsdk.operator.processing.event.EventSource;

public class ConnectorEvent extends AbstractEvent {
    private final String connectorName;
    private final String connectorNamespace;

    public ConnectorEvent(
        String relatedCustomResourceUid,
        EventSource eventSource,
        String connectorName,
        String connectorNamespace) {

        super(relatedCustomResourceUid, eventSource);

        this.connectorName = connectorName;
        this.connectorNamespace = connectorNamespace;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getConnectorNamespace() {
        return connectorNamespace;
    }
}
