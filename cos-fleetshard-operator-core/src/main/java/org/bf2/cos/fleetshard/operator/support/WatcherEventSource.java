package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WatcherEventSource<T> extends AbstractEventSource implements Watcher<T> {
    private final KubernetesClient client;
    private final Logger logger;

    private Watch watch;

    protected WatcherEventSource(KubernetesClient client) {
        this.client = client;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    protected abstract Watch watch();

    @Override
    public void start() {
        watch = watch();
    }

    @Override
    public void close() {
        if (watch != null) {
            try {
                logger.debug("Closing watch {}", watch);
                watch.close();
            } catch (Exception e) {
                logger.warn("Failed to close watch {}", watch, e);
            }
        }
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            return;
        }
        if (e.isHttpGone()) {
            logger.warn("Received error for watch, will try to reconnect.", e);
            close();
            watch();
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            logger.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }

    protected KubernetesClient getClient() {
        return client;
    }

    protected Logger getLogger() {
        return logger;
    }
}
