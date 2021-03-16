package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.EventSource;
import org.bf2.cos.fleetshard.api.connector.Connector;

public class ConnectorEvent<T extends Connector<?, ?>> extends DependantResourceEvent<T> {
    public ConnectorEvent(
            Watcher.Action action,
            T resource,
            String ownerUid,
            EventSource eventSource) {

        super(
                action,
                new ObjectReferenceBuilder()
                        .withNamespace(resource.getMetadata().getNamespace())
                        .withApiVersion(resource.getApiVersion())
                        .withKind(resource.getKind())
                        .withName(resource.getMetadata().getName())
                        .withUid(resource.getMetadata().getUid())
                        .withResourceVersion(resource.getMetadata().getResourceVersion())
                        .build(),
                ownerUid,
                eventSource);
    }
}