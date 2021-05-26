package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.fleet.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;

@Controller(
    name = "connector-cluster")
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    @Inject
    FleetManagerClient controlPlane;

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        boolean update = false;
        if (!cluster.getStatus().isReady()) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            update = true;
        }

        controlPlane.updateClusterStatus(cluster);

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }
}
