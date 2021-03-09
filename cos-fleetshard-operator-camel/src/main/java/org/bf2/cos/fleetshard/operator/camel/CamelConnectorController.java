package org.bf2.cos.fleetshard.operator.camel;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.camel.CamelConnector;

import io.javaoperatorsdk.operator.api.ResourceController;

@Controller
public class CamelConnectorController implements ResourceController<CamelConnector> {

    @Override
    public void init(EventSourceManager eventSourceManager) {
    }

    @Override
    public UpdateControl<CamelConnector> createOrUpdateResource(CamelConnector connector, Context<CamelConnector> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(CamelConnector connector, Context<CamelConnector> context) {
        return DeleteControl.DEFAULT_DELETE;
    }
}
