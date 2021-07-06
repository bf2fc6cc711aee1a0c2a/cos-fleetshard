package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWatcher<T> implements Watcher<T>, AutoCloseable {
    private final Logger logger;

    private Watch watch;

    protected AbstractWatcher() {
        this.logger = LoggerFactory.getLogger(getClass());
    }

    protected abstract Watch doWatch();

    protected abstract void onEventReceived(Action action, T resource);

    public void start() {
        this.watch = doWatch();
    }

    @Override
    public void close() {
        if (watch != null) {
            try {
                getLogger().debug("Closing watch {}", watch);
                watch.close();
            } catch (Exception e) {
                getLogger().warn("Failed to close watch {}", watch, e);
            }
        }
    }

    @Override
    public void eventReceived(Action action, T resource) {
        getLogger().debug("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        onEventReceived(action, resource);
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            return;
        }
        if (e.isHttpGone()) {
            logger.warn("Received error for watch, will try to reconnect.", e);
            close();
            start();
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            logger.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }

    protected Logger getLogger() {
        return logger;
    }
}
