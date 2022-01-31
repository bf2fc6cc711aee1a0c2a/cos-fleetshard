package org.bf2.cos.fleetshard.operator.support;

import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

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

    protected KubernetesClient getClient() {
        return client;
    }

    protected EventHandler getEventHandler() {
        return eventHandler;
    }

    @Override
    public void stop() throws OperatorException {
        close();
    }

}
