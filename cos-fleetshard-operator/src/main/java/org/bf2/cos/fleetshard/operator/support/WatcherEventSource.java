package org.bf2.cos.fleetshard.operator.support;

import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import io.javaoperatorsdk.operator.processing.event.EventSource;

public abstract class WatcherEventSource<T> extends AbstractWatcher<T> implements EventSource {
    private final KubernetesClient client;

    private EventHandler eventHandler;

    protected WatcherEventSource(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void eventSourceDeRegisteredForResource(String customResourceUid) {
    }

    protected KubernetesClient getClient() {
        return client;
    }

    protected EventHandler getEventHandler() {
        return eventHandler;
    }
}
