package org.bf2.synchronizer;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.cluster.ConnectorCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorClusterController implements ResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Override
    public void init(EventSourceManager eventSourceManager) {
    }

    @Override
    public UpdateControl<ConnectorCluster> createOrUpdateResource(ConnectorCluster connector, Context<ConnectorCluster> context) {
        LOGGER.info("createOrUpdateResource {}", connector);
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(ConnectorCluster connector, Context<ConnectorCluster> context) {
        LOGGER.info("deleteResource {}", connector);
        return DeleteControl.DEFAULT_DELETE;
    }
}
