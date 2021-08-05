package org.bf2.cos.fleetshard.operator.support;

import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import io.javaoperatorsdk.operator.processing.event.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RefreshEventSource implements EventSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshEventSource.class);

    private volatile boolean running;
    private volatile EventHandler eventHandler;

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void close() {
        this.running = false;
    }

    @Override
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void eventSourceDeRegisteredForResource(String s) {
    }

    public void refresh(String uid) {
        if (this.running && this.eventHandler != null) {
            LOGGER.debug("Triggering a refresh for resource with UID: {}", uid);
            this.eventHandler.handleEvent(new DefaultEvent(uid, this));
        }
    }
}
