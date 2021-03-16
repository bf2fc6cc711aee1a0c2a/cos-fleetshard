package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.camel.CamelConnectorEventSource;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumConnectorEventSource;
import org.bf2.cos.fleetshard.operator.sync.cp.ControlPlaneClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorClusterController implements ResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Inject
    @RestClient
    ControlPlaneClient controlPlane;
    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
                CamelConnectorEventSource.EVENT_SOURCE_ID,
                new CamelConnectorEventSource(client));
        eventSourceManager.registerEventSource(
                DebeziumConnectorEventSource.EVENT_SOURCE_ID,
                new DebeziumConnectorEventSource(client));
    }

    @Override
    public UpdateControl<ConnectorCluster> createOrUpdateResource(
            ConnectorCluster cluster,
            Context<ConnectorCluster> context) {

        for (Event e : context.getEvents().getList()) {
            // TODO: maybe update last resource version
        }

        LOGGER.info("createOrUpdateResource {}", cluster.getSpec());
        LOGGER.info("createOrUpdateResource {}", cluster.getStatus());
        // TODO: update control plane
        //       https://github.com/java-operator-sdk/java-operator-sdk/issues/369

        if (!cluster.getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready)) {
            cluster.getStatus().setPhase(ConnectorClusterStatus.PhaseType.Ready);

            LOGGER.info("createOrUpdateResource {}", cluster.getStatus());

            // does not work https://github.com/java-operator-sdk/java-operator-sdk/issues/371
            return UpdateControl.updateStatusSubResource(cluster);
        }

        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(
            ConnectorCluster connector,
            Context<ConnectorCluster> context) {

        LOGGER.info("deleteResource {}", connector);
        return DeleteControl.DEFAULT_DELETE;
    }
}
