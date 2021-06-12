package org.bf2.cos.fleetshard.operator.connectoroperator;

import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import io.javaoperatorsdk.operator.processing.event.EventSource;

public class ConnectorOperatorEvent extends AbstractEvent {
    public ConnectorOperatorEvent(String relatedCustomResourceUid, EventSource eventSource) {
        super(relatedCustomResourceUid, eventSource);
    }
}
