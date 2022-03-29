package org.bf2.cos.fleetshard.sync.resources;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleet.manager.model.ConnectorState;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleetshard.api.Conditions;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;

import io.fabric8.kubernetes.api.model.Condition;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_UNASSIGNED;

public class ConnectorStatusExtractor {
    public static ConnectorDeploymentStatus extract(ManagedConnector connector) {
        ConnectorDeploymentStatus status = new ConnectorDeploymentStatus();
        DeploymentSpec deployment = connector.getSpec().getDeployment();

        if (connector.getStatus() != null && connector.getStatus().getPhase() != null) {
            deployment = connector.getStatus().getDeployment();
        }

        status.setResourceVersion(deployment.getDeploymentResourceVersion());

        if (connector.getSpec().getOperatorSelector() == null || connector.getSpec().getOperatorSelector().getId() == null) {
            status.setPhase(ConnectorState.FAILED);
            status.addConditionsItem(new MetaV1Condition()
                .type(Conditions.TYPE_READY)
                .status(Conditions.STATUS_FALSE)
                .message("No assignable operator")
                .reason(Conditions.NO_ASSIGNABLE_OPERATOR_REASON)
                .lastTransitionTime(Conditions.now()));

            return status;
        }

        if (connector.getStatus() != null && connector.getStatus().getConnectorStatus() != null) {
            status.setOperators(
                new ConnectorDeploymentStatusOperators()
                    .assigned(
                        toConnectorOperator(connector.getStatus().getConnectorStatus().getAssignedOperator()))
                    .available(
                        toConnectorOperator(connector.getStatus().getConnectorStatus().getAvailableOperator())));

            if (connector.getStatus().getConnectorStatus() != null) {
                if (connector.getStatus().getConnectorStatus().getPhase() != null) {
                    status.setPhase(ConnectorState.fromValue(
                        connector.getStatus().getConnectorStatus().getPhase()));
                }
                if (connector.getStatus().getConnectorStatus().getConditions() != null) {
                    for (var cond : connector.getStatus().getConnectorStatus().getConditions()) {
                        status.addConditionsItem(toMetaV1Condition(cond));
                    }
                }
            }
        }

        if (status.getPhase() == null) {
            status.setPhase(ConnectorState.PROVISIONING);

            if (DESIRED_STATE_DELETED.equals(deployment.getDesiredState())) {
                status.setPhase(ConnectorState.DEPROVISIONING);
            } else if (DESIRED_STATE_STOPPED.equals(deployment.getDesiredState())) {
                status.setPhase(ConnectorState.DEPROVISIONING);
            } else if (DESIRED_STATE_UNASSIGNED.equals(deployment.getDesiredState())) {
                status.setPhase(ConnectorState.DEPROVISIONING);
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
