package org.bf2.cos.fleetshard.operator.debezium;

import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class DebeziumConnectorController implements ResourceController<DebeziumConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumConnectorController.class);

    @Override
    public void init(EventSourceManager eventSourceManager) {
    }

    @Override
    public UpdateControl<DebeziumConnector> createOrUpdateResource(
            DebeziumConnector connector,
            Context<DebeziumConnector> context) {

        LOGGER.info("createOrUpdateResource {}", connector);
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(
            DebeziumConnector connector,
            Context<DebeziumConnector> context) {

        LOGGER.info("deleteResource {}", connector);
        return DeleteControl.DEFAULT_DELETE;
    }
}
