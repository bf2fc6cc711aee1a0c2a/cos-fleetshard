package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;

public abstract class AbstractResourceController<R extends CustomResource> implements ResourceController<R> {
    private final TimerEventSource retryTimer;

    private EventSourceManager eventSourceManager;

    public AbstractResourceController() {
        this.retryTimer = new TimerEventSource();
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        this.eventSourceManager = eventSourceManager;
        this.eventSourceManager.registerEventSource("_timer", retryTimer);

        registerEventSources(this.eventSourceManager);
    }

    @Override
    public DeleteControl deleteResource(R resource, Context<R> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    protected TimerEventSource getRetryTimer() {
        return retryTimer;
    }

    protected EventSourceManager getEventSourceManager() {
        return eventSourceManager;
    }

    protected void registerEventSources(EventSourceManager eventSourceManager) {
    }
}
