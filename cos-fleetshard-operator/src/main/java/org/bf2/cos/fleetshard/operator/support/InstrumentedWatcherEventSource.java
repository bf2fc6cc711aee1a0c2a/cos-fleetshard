package org.bf2.cos.fleetshard.operator.support;

import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class InstrumentedWatcherEventSource<T extends HasMetadata> extends WatcherEventSource<T> {
    private final ResourceAwareMetricsRecorder recorder;

    protected InstrumentedWatcherEventSource(KubernetesClient client, ResourceAwareMetricsRecorder recorder) {
        super(client);
        this.recorder = recorder;
    }

    @Override
    public void eventReceived(Action action, T resource) {
        if (action == Action.ERROR) {
            getLogger().warn("Skipping ERROR event received for action: {}", action.name());
            return;
        }

        this.recorder.record(resource, () -> onEventReceived(action, resource));
    }
}
