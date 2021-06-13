package org.bf2.cos.fleetshard.operator.support;

import org.bf2.cos.fleet.manager.api.model.cp.ConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;

public final class OperatorSupport {
    private OperatorSupport() {
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
}
