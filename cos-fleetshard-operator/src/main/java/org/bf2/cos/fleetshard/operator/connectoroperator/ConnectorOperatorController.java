package org.bf2.cos.fleetshard.operator.connectoroperator;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkiverse.operatorsdk.runtime.DelayRegistrationUntil;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.FleetShardEvents;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(name = "connector-operator", finalizerName = Controller.NO_FINALIZER, generationAwareEventProcessing = false)
@DelayRegistrationUntil(event = FleetShardEvents.Started.class)
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
