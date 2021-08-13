package org.bf2.cos.fleetshard.sync.connector;

import io.fabric8.kubernetes.api.model.Condition;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;

public class ConnectorStatusExtractor {
    public static ConnectorDeploymentStatus extract(ManagedConnector connector) {
        ConnectorDeploymentStatus status = new ConnectorDeploymentStatus();
        DeploymentSpec deployment = connector.getSpec().getDeployment();

        if (connector.getStatus() != null && connector.getStatus().getPhase() != null) {
            deployment = connector.getStatus().getDeployment();
        }

        status.setResourceVersion(deployment.getDeploymentResourceVersion());

        if (connector.getStatus() != null && connector.getStatus().getConnectorStatus() != null) {
            status.setOperators(
                new ConnectorDeploymentStatusOperators()
                    .assigned(
                        toConnectorOperator(connector.getStatus().getConnectorStatus().getAssignedOperator()))
                    .available(
                        toConnectorOperator(connector.getStatus().getConnectorStatus().getAvailableOperator())));

            if (connector.getStatus().getConnectorStatus() != null) {
                if (connector.getStatus().getConnectorStatus().getPhase() != null) {
                    status.setPhase(connector.getStatus().getConnectorStatus().getPhase());
                }
                if (connector.getStatus().getConnectorStatus().getConditions() != null) {
                    for (var cond : connector.getStatus().getConnectorStatus().getConditions()) {
                        status.addConditionsItem(toMetaV1Condition(cond));
                    }
                }
            }
        }

        if (status.getPhase() == null) {
            if (DESIRED_STATE_DELETED.equals(deployment.getDesiredState())) {
                status.setPhase(STATE_DE_PROVISIONING);
            } else if (DESIRED_STATE_STOPPED.equals(deployment.getDesiredState())) {
                status.setPhase(STATE_DE_PROVISIONING);
            } else {
                status.setPhase(STATE_PROVISIONING);
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
