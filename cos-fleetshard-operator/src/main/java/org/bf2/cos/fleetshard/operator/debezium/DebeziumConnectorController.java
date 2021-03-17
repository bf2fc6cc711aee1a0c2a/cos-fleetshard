package org.bf2.cos.fleetshard.operator.debezium;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class DebeziumConnectorController extends AbstractResourceController<DebeziumConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumConnectorController.class);

    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        // TODO: watch owned resource
    }

    @Override
    public UpdateControl<DebeziumConnector> createOrUpdateResource(
            DebeziumConnector connector,
            Context<DebeziumConnector> context) {

        LOGGER.info("createOrUpdateResource {}", connector);
        return UpdateControl.noUpdate();
    }
}
