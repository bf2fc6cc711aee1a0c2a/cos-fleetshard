package org.bf2.cos.fleetshard.operator.connector;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.it.support.WatcherEventSource;

public abstract class ConnectorEventSource extends WatcherEventSource<ManagedConnector> {
    private final String namespace;

    public ConnectorEventSource(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient);

        this.namespace = namespace;
    }

    @Override
    protected Watch watch() {
        return getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .watch(this);
    }

    @Override
    public void eventReceived(Action action, ManagedConnector resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        getLogger().info("Event {} received on connector: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        resourceUpdated(resource);
    }

    protected abstract void resourceUpdated(ManagedConnector resource);
}
