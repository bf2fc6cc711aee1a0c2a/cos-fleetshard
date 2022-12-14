package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.ConfigMaps;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectorConfigMapWatcher extends AbstractWatcher<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfigMapWatcher.class);

    private final ManagedConnectorOperator operator;
    private final EventClient eventClient;
    private final KubernetesClient kubernetesClient;
    private final ResourceAwareMetricsRecorder recorder;

    public ConnectorConfigMapWatcher(
        KubernetesClient kubernetesClient,
        ManagedConnectorOperator operator,
        ResourceAwareMetricsRecorder recorder,
        EventClient eventClient) {

        this.kubernetesClient = kubernetesClient;
        this.operator = operator;
        this.eventClient = eventClient;
        this.recorder = recorder;
    }

    @Override
    protected Watch doWatch() {
        LOGGER.info("Creating Watcher for Connector ConfigMaps.");

        return kubernetesClient
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

        if (Action.MODIFIED.equals(action)) {
            this.recorder.record(resource, () -> updateManagedConnectorResource(resource));
        }
    }

    private void updateManagedConnectorResource(ConfigMap resource) {
        String checksum = resource.getData() == null ? null : ConfigMaps.computeChecksum(resource);

        ResourceID.fromFirstOwnerReference(resource)
            .filter(rid -> rid.getName() != null && rid.getNamespace().isPresent())
            .ifPresent(resourceID -> {
                // do a broadcast to provide feedback since changing this configmap is expected to be manual operation
                eventClient.broadcastNormal(
                    "ConnectorConfigMap",
                    "Updating ManagedConnector (%s/%s) after it's ConfigMap has been modified.",
                    resource,
                    resourceID.getNamespace().get(),
                    resourceID.getName());

                kubernetesClient.resources(ManagedConnector.class)
                    .inNamespace(resourceID.getNamespace().get())
                    .withName(resourceID.getName())
                    .accept(
                        mctr -> {
                            LOGGER.info("Updating ManagedConnector ({}/{}) configMapChecksum to {}",
                                mctr.getMetadata().getNamespace(),
                                mctr.getMetadata().getName(),
                                checksum);
                            mctr.getSpec().getDeployment()
                                .setConfigMapChecksum(checksum);
                        });
            });
    }
}
