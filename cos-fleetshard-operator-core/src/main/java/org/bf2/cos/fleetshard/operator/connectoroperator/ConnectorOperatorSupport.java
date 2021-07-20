package org.bf2.cos.fleetshard.operator.connectoroperator;

import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;

public final class ConnectorOperatorSupport {
    private ConnectorOperatorSupport() {
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

    public static ConnectorOperator toConnectorOperator(ManagedConnectorOperator operator) {
        if (operator == null) {
            return null;
        }

        return new ConnectorOperator()
            .id(operator.getMetadata().getName())
            .type(operator.getSpec().getType())
            .version(operator.getSpec().getVersion());
    }
}
