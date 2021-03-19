package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceEventSource extends AbstractEventSource implements Watcher<String> {
    private final KubernetesClient client;
    private final Logger logger;

    protected ResourceEventSource(KubernetesClient client) {
        this.client = client;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void setEventHandler(EventHandler eventHandler) {
        super.setEventHandler(eventHandler);
        watch();
    }

    protected abstract void watch();

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            return;
        }
        if (e.isHttpGone()) {
            logger.warn("Received error for watch, will try to reconnect.", e);
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
