package org.bf2.cos.fleetshard.operator.support;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import io.javaoperatorsdk.operator.processing.event.EventSource;

import static org.bf2.cos.fleetshard.support.ResourceUtil.objectRef;
import static org.bf2.cos.fleetshard.support.ResourceUtil.ownerUid;

public class ObjectReferenceResourceEvent extends AbstractEvent {
    private final Watcher.Action action;
    private final ObjectReference objectReference;

    public ObjectReferenceResourceEvent(
        Watcher.Action action,
        ObjectReference objectReference,
        String ownerUid,
        EventSource eventSource) {

        super(
            Objects.requireNonNull(ownerUid),
            Objects.requireNonNull(eventSource));

        this.action = action;
        this.objectReference = objectReference;
    }

    public ObjectReferenceResourceEvent(
        Watcher.Action action,
        HasMetadata resource,
        EventSource eventSource) {

        this(action, objectRef(resource), ownerUid(resource), eventSource);
    }

    public Watcher.Action getAction() {
        return action;
    }

    public ObjectReference getObjectReference() {
        return objectReference;
    }
}