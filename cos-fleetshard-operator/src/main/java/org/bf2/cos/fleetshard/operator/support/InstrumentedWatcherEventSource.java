package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class InstrumentedWatcherEventSource<T> extends WatcherEventSource<T> {
    private final MetricsRecorder recorder;

    protected InstrumentedWatcherEventSource(KubernetesClient client, MetricsRecorder recorder) {
        super(client);
        this.recorder = recorder;
    }

    @Override
    public void eventReceived(Action action, T resource) {
        if (action == Action.ERROR) {
            getLogger().warn("Skipping ERROR event received for action: {}", action.name());
            return;
        }

        this.recorder.record(() -> onEventReceived(action, resource));
    }
}
