package org.bf2.cos.fleetshard.operator.connectoroperator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConnectorOperatorEventSource extends WatcherEventSource<ManagedConnectorOperator> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorOperatorEventSource.class);

    private final String namespace;

    public ConnectorOperatorEventSource(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient);

        this.namespace = namespace;
    }

    @Override
    protected Watch doWatch() {
        return getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(this.namespace)
            .watch(this);
    }

    @Override
    protected void onEventReceived(Action action, ManagedConnectorOperator resource) {
        LOGGER.debug("Event {} received on operator: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        resourceUpdated(resource);
    }

    protected abstract void resourceUpdated(ManagedConnectorOperator resource);
}
