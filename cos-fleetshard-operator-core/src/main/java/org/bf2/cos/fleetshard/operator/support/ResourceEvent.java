package org.bf2.cos.fleetshard.operator.support;

import java.util.Objects;

import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import io.javaoperatorsdk.operator.processing.event.EventSource;
import org.bf2.cos.fleetshard.api.ResourceRef;

public class ResourceEvent extends DefaultEvent {
    private final Watcher.Action action;
    private final ResourceRef resourceRef;

    public ResourceEvent(
        Watcher.Action action,
        ResourceRef objectReference,
        String ownerUid,
        EventSource eventSource) {

        super(
            Objects.requireNonNull(ownerUid),
            Objects.requireNonNull(eventSource));

        this.action = action;
        this.resourceRef = objectReference;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public ResourceRef getResourceRef() {
        return resourceRef;
    }
}