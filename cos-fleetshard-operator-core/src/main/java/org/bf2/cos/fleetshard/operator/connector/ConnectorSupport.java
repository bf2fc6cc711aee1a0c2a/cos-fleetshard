package org.bf2.cos.fleetshard.operator.connector;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.OperatorSelector;

public final class ConnectorSupport {
    private ConnectorSupport() {
    }

    public static OperatorSelector getOperatorSelector(ConnectorDeployment deployment) {
        ArrayNode operatorsMeta = deployment.getSpec().getShardMetadata().withArray("operators");
        if (operatorsMeta.size() != 1) {
            throw new IllegalArgumentException("Multiple selectors are not yet supported");
        }

        return new OperatorSelector(
            deployment.getSpec().getOperatorId(),
            operatorsMeta.get(0).requiredAt("/type").asText(),
            operatorsMeta.get(0).requiredAt("/version").asText());
    }
}
