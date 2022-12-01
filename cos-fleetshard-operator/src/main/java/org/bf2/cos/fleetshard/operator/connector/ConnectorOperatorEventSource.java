package org.bf2.cos.fleetshard.operator.connector;

import java.util.Objects;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.operator.support.InstrumentedWatcherEventSource;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectorOperatorEventSource extends InstrumentedWatcherEventSource<ManagedConnectorOperator> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorOperatorEventSource.class);

    private final ManagedConnectorOperator operator;
    private final String operatorsNamespace;

    public ConnectorOperatorEventSource(
        KubernetesClient kubernetesClient,
        ManagedConnectorOperator operator,
        String operatorsNamespace,
        ResourceAwareMetricsRecorder recorder) {

        super(kubernetesClient, recorder);

        this.operator = operator;
        this.operatorsNamespace = operatorsNamespace;
    }

    @Override
    protected Watch doWatch() {
        return getClient()
            .resources(ManagedConnectorOperator.class)
            .inNamespace(operatorsNamespace)
            .withLabel(Resources.LABEL_OPERATOR_TYPE, operator.getSpec().getType())
            .watch(this);
    }

    @Override
    protected void onEventReceived(Action action, ManagedConnectorOperator resource) {
        LOGGER.debug("Event {} received for managed connector operator: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        getClient()
            .resources(ManagedConnector.class)
            .inAnyNamespace()
            .withLabel(Resources.LABEL_OPERATOR_TYPE, resource.getSpec().getType())
            .list().getItems().stream()
            .filter(mc -> mc.getStatus() != null)
            .filter(mc -> mc.getStatus().getConnectorStatus().getAssignedOperator() != null)
            .filter(mc -> isSameOperatorType(resource, mc))
            .filter(mc -> hasGreaterOperatorVersion(resource, mc))
            .map(ResourceID::fromResource)
            .forEach(resourceId -> getEventHandler().handleEvent(new Event(resourceId)));
    }

    private boolean isSameOperatorType(ManagedConnectorOperator resource, ManagedConnector mc) {
        return Objects.equals(resource.getSpec().getType(),
            mc.getStatus().getConnectorStatus().getAssignedOperator().getType());
    }

    private boolean hasGreaterOperatorVersion(ManagedConnectorOperator resource, ManagedConnector mc) {
        final var rv = new Version(resource.getSpec().getVersion());
        final var cv = new Version(mc.getStatus().getConnectorStatus().getAssignedOperator().getVersion());
        return rv.compareTo(cv) > 0;
    }

}
