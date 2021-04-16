package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Controller(
    name = "connector-cluster")
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    @Inject
    ControlPlane controlPlane;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String namespace;

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        final var id = cluster.getStatus().getId();
        final var ns = cluster.getStatus().getConnectorsNamespace();
        final var ph = cluster.getStatus().getPhase();

        boolean update = false;

        if (!Objects.equals(ph, ManagedConnectorClusterStatus.PhaseType.Ready)
            || !Objects.equals(id, clusterId)
            || Objects.equals(ns, namespace)) {

            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            cluster.getStatus().setId(clusterId);
            cluster.getStatus().setConnectorsNamespace(namespace);

            update = true;
        }

        controlPlane.updateClusterStatus(cluster);

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }
}
