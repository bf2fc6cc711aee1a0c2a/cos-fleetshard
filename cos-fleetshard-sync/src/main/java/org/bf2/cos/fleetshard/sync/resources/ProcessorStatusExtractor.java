package org.bf2.cos.fleetshard.sync.resources;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleet.manager.model.ProcessorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ProcessorState;
import org.bf2.cos.fleetshard.api.Conditions;
import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.api.Operator;

import io.fabric8.kubernetes.api.model.Condition;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_UNASSIGNED;

public class ProcessorStatusExtractor {
    public static ProcessorDeploymentStatus extract(ManagedProcessor processor) {
        ProcessorDeploymentStatus status = new ProcessorDeploymentStatus();

        status.setResourceVersion(processor.getSpec().getDeploymentResourceVersion());

        if (processor.getSpec().getOperatorSelector() == null || processor.getSpec().getOperatorSelector().getId() == null) {
            status.setPhase(ProcessorState.FAILED);
            status.addConditionsItem(new MetaV1Condition()
                .type(Conditions.TYPE_READY)
                .status(Conditions.STATUS_FALSE)
                .message("No assignable operator")
                .reason(Conditions.NO_ASSIGNABLE_OPERATOR_REASON)
                .lastTransitionTime(Conditions.now()));

            return status;
        }

        if (processor.getStatus() != null && processor.getStatus().getProcessorStatus() != null) {
            status.setOperators(
                new ConnectorDeploymentStatusOperators()
                    .assigned(
                        toConnectorOperator(processor.getStatus().getProcessorStatus().getAssignedOperator()))
                    .available(
                        toConnectorOperator(processor.getStatus().getProcessorStatus().getAvailableOperator())));

            if (processor.getStatus().getProcessorStatus() != null) {
                if (processor.getStatus().getProcessorStatus().getPhase() != null) {
                    status.setPhase(ProcessorState.fromValue(
                        processor.getStatus().getProcessorStatus().getPhase()));
                }
                if (processor.getStatus().getProcessorStatus().getConditions() != null) {
                    for (var cond : processor.getStatus().getProcessorStatus().getConditions()) {
                        status.addConditionsItem(toMetaV1Condition(cond));
                    }
                }
            }
        }

        if (status.getPhase() == null) {
            status.setPhase(ProcessorState.PROVISIONING);

            if (DESIRED_STATE_DELETED.equals(processor.getSpec().getDesiredState())) {
                status.setPhase(ProcessorState.DEPROVISIONING);
            } else if (DESIRED_STATE_STOPPED.equals(processor.getSpec().getDesiredState())) {
                status.setPhase(ProcessorState.DEPROVISIONING);
            } else if (DESIRED_STATE_UNASSIGNED.equals(processor.getSpec().getDesiredState())) {
                status.setPhase(ProcessorState.DEPROVISIONING);
            }
        }

        return status;
    }

    public static ConnectorOperator toConnectorOperator(Operator operator) {
        if (operator == null) {
            return null;
        }

        return new ConnectorOperator()
            .id(operator.getId())
            .type(operator.getType())
            .version(operator.getVersion());
    }

    public static MetaV1Condition toMetaV1Condition(Condition condition) {
        return new MetaV1Condition()
            .type(condition.getType())
            .status(condition.getStatus())
            .message(condition.getMessage())
            .reason(condition.getReason())
            .lastTransitionTime(condition.getLastTransitionTime());
    }
}
