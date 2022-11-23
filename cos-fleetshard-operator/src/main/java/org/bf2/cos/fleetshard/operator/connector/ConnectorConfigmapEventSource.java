package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.InstrumentedWatcherEventSource;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.ConfigMaps;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectorConfigmapEventSource extends InstrumentedWatcherEventSource<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfigmapEventSource.class);

    private final ManagedConnectorOperator operator;
    private final EventClient eventClient;

    public ConnectorConfigmapEventSource(
        KubernetesClient kubernetesClient,
        ManagedConnectorOperator operator,
        MetricsRecorder recorder,
        EventClient eventClient) {

        super(kubernetesClient, recorder);

        this.operator = operator;
        this.eventClient = eventClient;
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

        final String configMapChecksum;
        if (resource.getData() == null) {
            configMapChecksum = null;
        } else {
            switch (action) {
                case DELETED:
                    configMapChecksum = null;
                    break;
                default:
                    configMapChecksum = ConfigMaps.computeChecksum(resource);
                    break;
            }
        }

        ResourceID.fromFirstOwnerReference(resource)
            .filter(rid -> rid.getName() != null && rid.getNamespace().isPresent())
            .ifPresent(resourceID -> {
                // do a broadcast to provide feedback since changing this configmap is expected to be manual operation
                eventClient.broadcastNormal(
                    "ConnectorConfigMap",
                    "Updating ManagedConnector (%s/%s) after event %s from configmap.",
                    resource,
                    resourceID.getNamespace().get(),
                    resourceID.getName(),
                    action.name());

                getClient().resources(ManagedConnector.class)
                    .inNamespace(resourceID.getNamespace().get())
                    .withName(resourceID.getName())
                    .accept(
                        mctr -> {
                            LOGGER.info("Updating ManagedConnector ({}/{}) configMapChecksum to {}",
                                mctr.getMetadata().getNamespace(),
                                mctr.getMetadata().getName(),
                                configMapChecksum);
                            mctr.getSpec().getDeployment()
                                .setConfigMapChecksum(configMapChecksum);
                        });
            });
    }
}
