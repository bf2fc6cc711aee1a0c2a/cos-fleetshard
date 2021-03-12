package org.bf2.cos.fleetshard.operator.camel;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class CamelConnectorController implements ResourceController<CamelConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelConnectorController.class);

    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
    }

    @Override
    public UpdateControl<CamelConnector> createOrUpdateResource(
            CamelConnector connector,
            Context<CamelConnector> context) {

        LOGGER.info("createOrUpdateResource {}", connector);
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(
            CamelConnector connector,
            Context<CamelConnector> context) {

        LOGGER.info("deleteResource {}", connector);
        return DeleteControl.DEFAULT_DELETE;
    }
}
