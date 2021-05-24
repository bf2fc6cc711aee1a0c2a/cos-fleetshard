package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.fleet.FleetManager;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;

@Controller(
    name = "connector-cluster")
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetManager controlPlane;

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        boolean update = false;
        if (!Objects.equals(cluster.getStatus().getPhase(), ManagedConnectorClusterStatus.PhaseType.Ready)) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            update = true;
        }

        controlPlane.updateClusterStatus(cluster);

        var connectors = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(cluster.getSpec().getConnectorsNamespace())
            .list();

        for (var connector : connectors.getItems()) {
            //TODO: check connectors that can be moved to a newer operator
        }

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }
}
