package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEvent;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEventSource;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;

@Controller(
    name = "connector-cluster",
    finalizerName = Controller.NO_FINALIZER,
    generationAwareEventProcessing = false)
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    @Inject
    FleetShardClient fleetShard;
    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void registerEventSources(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_operators",
            new ConnectorOperatorEventSource(kubernetesClient, fleetShard.getClusterNamespace()) {
                @Override
                protected void resourceUpdated(ManagedConnectorOperator resource) {
                    fleetShard.lookupManagedConnectorCluster()
                        .map(cluster -> new ConnectorOperatorEvent(cluster.getMetadata().getUid(), this))
                        .ifPresent(event -> getEventHandler().handleEvent(event));
                }
            });
    }

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        if (!Objects.equals(cluster.getSpec().getId(), fleetShard.getClusterId())) {
            return UpdateControl.noUpdate();
        }

        boolean update = false;
        if (fleetShard.lookupManagedConnectorOperators().isEmpty()) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Unconnected);
            update = true;
        } else if (!cluster.getStatus().isReady()) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            update = true;
        }

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }
}
