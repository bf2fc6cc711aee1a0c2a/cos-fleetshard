package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.InstrumentedWatcherEventSource;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectorConfigmapEventSource extends InstrumentedWatcherEventSource<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfigmapEventSource.class);

    private final ManagedConnectorOperator operator;

    public ConnectorConfigmapEventSource(
        KubernetesClient kubernetesClient,
        ManagedConnectorOperator operator,
        MetricsRecorder recorder) {

        super(kubernetesClient, recorder);

        this.operator = operator;
    }

    @Override
    protected Watch doWatch() {
        return getClient()
            .configMaps()
            .inAnyNamespace()
            .withLabel(Resources.LABEL_OPERATOR_TYPE, operator.getSpec().getType())
            .watch(this);
    }

    @Override
    protected void onEventReceived(Action action, ConfigMap resource) {
        LOGGER.debug("Event {} received for configmap: {}/{}",
            action.name(),
            resource.getMetadata().getNamespace(),
            resource.getMetadata().getName());

        ResourceID.fromFirstOwnerReference(resource).ifPresent(rid -> {
            getEventHandler().handleEvent(new Event(rid));
        });
    }
}
