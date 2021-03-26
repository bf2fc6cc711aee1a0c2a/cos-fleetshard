package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.ConnectorCluster;
import org.bf2.cos.fleetshard.api.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEventSource;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorClusterController extends AbstractResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

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
        if (!cluster.getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready)) {
            cluster.getStatus().setPhase(ConnectorClusterStatus.PhaseType.Ready.name());
        }

        controlPlane.updateClusterStatus(cluster);

        return UpdateControl.updateStatusSubResource(cluster);
    }
}
