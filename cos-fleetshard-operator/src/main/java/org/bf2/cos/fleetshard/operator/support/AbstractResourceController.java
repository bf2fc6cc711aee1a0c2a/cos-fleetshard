package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;

public abstract class AbstractResourceController<R extends CustomResource> implements ResourceController<R> {
    private final TimerEventSource retryTimer;
    private final RefreshEventSource refresher;

    private volatile EventSourceManager eventSourceManager;

    protected AbstractResourceController() {
        this.retryTimer = new TimerEventSource();
        this.refresher = new RefreshEventSource();
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        this.eventSourceManager = eventSourceManager;
        this.eventSourceManager.registerEventSource("_timer", retryTimer);
        this.eventSourceManager.registerEventSource("_refresh", refresher);

        registerEventSources(this.eventSourceManager);
    }

    @Override
    public DeleteControl deleteResource(R resource, Context<R> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    public void refresher(String uid) {
        this.refresher.refresh(uid);
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
