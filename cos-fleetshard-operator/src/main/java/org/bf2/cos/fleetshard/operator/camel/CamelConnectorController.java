package org.bf2.cos.fleetshard.operator.camel;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class CamelConnectorController extends AbstractResourceController<CamelConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelConnectorController.class);

    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        // TODO: watch owned resource
    }

    @Override
    public UpdateControl<CamelConnector> createOrUpdateResource(
            CamelConnector connector,
            Context<CamelConnector> context) {

        try {
            LOGGER.info("createOrUpdateResource spec: {}",
                    connector.getSpec());
            LOGGER.info("createOrUpdateResource spec: {}",
                    Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(connector.getSpec()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return UpdateControl.noUpdate();
    }
}
