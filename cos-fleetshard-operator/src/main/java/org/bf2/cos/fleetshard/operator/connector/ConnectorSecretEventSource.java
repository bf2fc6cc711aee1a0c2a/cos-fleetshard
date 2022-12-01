package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.InstrumentedWatcherEventSource;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectorSecretEventSource extends InstrumentedWatcherEventSource<Secret> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSecretEventSource.class);

    private final ManagedConnectorOperator operator;

    public ConnectorSecretEventSource(
        KubernetesClient kubernetesClient,
        ManagedConnectorOperator operator,
        ResourceAwareMetricsRecorder recorder) {

        super(kubernetesClient, recorder);

        this.operator = operator;
    }

    @Override
    protected Watch doWatch() {
        return getClient()
            .secrets()
            .inAnyNamespace()
            .withLabel(Resources.LABEL_OPERATOR_TYPE, operator.getSpec().getType())
            .withLabel(Resources.LABEL_UOW)
            .watch(this);
    }

    @Override
    protected void onEventReceived(Action action, Secret resource) {
        LOGGER.debug("Event {} received for secret: {}/{} ({})",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName(),
            resource.getMetadata().getResourceVersion());

        ResourceID.fromFirstOwnerReference(resource).ifPresent(rid -> {
            getEventHandler().handleEvent(new Event(rid));
        });
    }
}
