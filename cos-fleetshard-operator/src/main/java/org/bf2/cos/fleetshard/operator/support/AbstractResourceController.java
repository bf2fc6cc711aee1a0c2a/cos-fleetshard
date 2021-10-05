package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.DefaultEventSourceManager;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;

public abstract class AbstractResourceController<R extends CustomResource> implements ResourceController<R> {
    private final RefreshEventSource refresher;

    private volatile EventSourceManager eventSourceManager;

    protected AbstractResourceController() {
        this.refresher = new RefreshEventSource();
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        this.eventSourceManager = eventSourceManager;
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

    @SuppressWarnings("rawtypes")
    protected TimerEventSource getRetryTimer() {
        return (TimerEventSource) eventSourceManager.getRegisteredEventSources()
            .get(DefaultEventSourceManager.RETRY_TIMER_EVENT_SOURCE_NAME);
    }

    protected EventSourceManager getEventSourceManager() {
        return eventSourceManager;
    }

    protected void registerEventSources(EventSourceManager eventSourceManager) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public UpdateControl<R> createOrUpdateResource(R resource, Context<R> context) {
        TimerEventSource tes = getRetryTimer();
        if (tes != null) {
            getRetryTimer().cancelOnceSchedule(resource.getMetadata().getUid());
        }

        return reconcile(resource, context);
    }

    protected abstract UpdateControl<R> reconcile(R resource, Context<R> context);
}
