package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.operator.camel.CamelConnectorEventSource;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumConnectorEventSource;
import org.bf2.cos.fleetshard.operator.sync.ConnectorsControlPlane;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class ConnectorClusterController implements ResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Inject
    @RestClient
    ConnectorsControlPlane controlPlane;
    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        LOGGER.info("client: {}", client);
        LOGGER.info("controlPlane: {}", controlPlane);

        eventSourceManager.registerEventSource(
                CamelConnectorEventSource.EVENT_SOURCE_ID,
                new CamelConnectorEventSource(client));
        eventSourceManager.registerEventSource(
                DebeziumConnectorEventSource.EVENT_SOURCE_ID,
                new DebeziumConnectorEventSource(client));
    }

    @Override
    public UpdateControl<ConnectorCluster> createOrUpdateResource(
            ConnectorCluster connector,
            Context<ConnectorCluster> context) {

        LOGGER.info("createOrUpdateResource {}", connector);
        // TODO: update control plane

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
