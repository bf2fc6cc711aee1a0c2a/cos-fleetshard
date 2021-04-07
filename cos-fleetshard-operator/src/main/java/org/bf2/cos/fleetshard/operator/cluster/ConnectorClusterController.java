package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleet.manager.api.model.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ConnectorCluster;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEventSource;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;

@Controller
public class ConnectorClusterController extends AbstractResourceController<ConnectorCluster> {
    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        // set up a trigger to react to connector changes
        // TODO: is this needed ?
        eventSourceManager.registerEventSource(
                ConnectorEventSource.EVENT_SOURCE_ID,
                new ConnectorEventSource(kubernetesClient));
    }

    @Override
    public UpdateControl<ConnectorCluster> createOrUpdateResource(
            ConnectorCluster cluster,
            Context<ConnectorCluster> context) {

        if (cluster.getStatus() == null) {
            cluster.setStatus(new ConnectorClusterStatus());
        }
        if (!Objects.equals(cluster.getStatus().getPhase(), "ready")) {
            cluster.getStatus().setPhase("ready");
        }

        controlPlane.updateClusterStatus(cluster);

        return UpdateControl.updateStatusSubResource(cluster);
    }
}
