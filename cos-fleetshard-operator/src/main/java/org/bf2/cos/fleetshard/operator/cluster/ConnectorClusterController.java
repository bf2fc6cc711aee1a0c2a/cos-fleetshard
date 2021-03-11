package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.operator.sync.ConnectorsControlPlane;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorClusterController implements ResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Inject
    @RestClient
    ConnectorsControlPlane controlPlane;

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
