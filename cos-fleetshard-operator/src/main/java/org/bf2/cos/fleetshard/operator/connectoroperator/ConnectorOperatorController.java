package org.bf2.cos.fleetshard.operator.connectoroperator;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller(name = "connector-operator", finalizerName = Controller.NO_FINALIZER, generationAwareEventProcessing = false)
public class ConnectorOperatorController extends AbstractResourceController<ManagedConnectorOperator> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorOperatorController.class);

    @Override
    public UpdateControl<ManagedConnectorOperator> createOrUpdateResource(
        ManagedConnectorOperator connectorOperator,
        Context<ManagedConnectorOperator> context) {

        LOGGER.info("{}", connectorOperator);

        return UpdateControl.noUpdate();
    }
}
