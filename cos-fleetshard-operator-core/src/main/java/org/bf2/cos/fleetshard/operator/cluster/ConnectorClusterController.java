package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;

@Controller(
    name = "connector-cluster",
    finalizerName = Controller.NO_FINALIZER,
    generationAwareEventProcessing = false)
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        if (!Objects.equals(cluster.getSpec().getId(), fleetShard.getClusterId())) {
            return UpdateControl.noUpdate();
        }

        boolean update = false;
        if (!cluster.getStatus().isReady()) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            update = true;
        }

        controlPlane.updateClusterStatus(
            cluster,
            fleetShard.lookupManagedConnectorOperators());

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }
}
