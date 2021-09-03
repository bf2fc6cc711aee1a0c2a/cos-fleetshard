package org.bf2.cos.fleetshard.operator.connectoroperator;

import java.util.Objects;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class ConnectorOperatorEventSource extends WatcherEventSource<ManagedConnectorOperator> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorOperatorEventSource.class);

    private final ManagedConnectorOperator operator;
    private final String namespace;

    public ConnectorOperatorEventSource(KubernetesClient kubernetesClient, ManagedConnectorOperator operator,
        String namespace) {
        super(kubernetesClient);
        this.operator = operator;
        this.namespace = namespace;
    }

    @Override
    protected Watch doWatch() {
        return getClient()
            .resources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .withLabel(Resources.LABEL_OPERATOR_TYPE, operator.getSpec().getType())
            .watch(this);
    }

    @Override
    protected void onEventReceived(Action action, ManagedConnectorOperator resource) {
        LOGGER.debug("Event {} received for managed connector operator: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        getEventHandler().handleEvent(
            new DefaultEvent(
                cr -> hasGreaterVersion((ManagedConnector) cr, resource),
                this));
    }

    private boolean hasGreaterVersion(ManagedConnector connector, ManagedConnectorOperator resource) {
        if (connector.getStatus() == null) {
            return false;
        }
        if (connector.getStatus().getConnectorStatus().getAssignedOperator() == null) {
            return false;
        }
        if (!Objects.equals(resource.getSpec().getType(),
            connector.getStatus().getConnectorStatus().getAssignedOperator().getType())) {
            return false;
        }

        final var rv = new Version(resource.getSpec().getVersion());
        final var cv = new Version(connector.getStatus().getConnectorStatus().getAssignedOperator().getVersion());

        return rv.compareTo(cv) > 0;
    }
}
