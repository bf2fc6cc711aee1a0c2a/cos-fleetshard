package org.bf2.cos.fleetshard.operator.support;

import org.bf2.cos.fleet.manager.api.model.ConnectorDeployment;

public final class ConnectorDeploymentSupport {
    private ConnectorDeploymentSupport() {
    }

    public static long getResourceVersion(ConnectorDeployment connectorDeployment) {
        if (connectorDeployment.getMetadata() == null) {
            throw new IllegalArgumentException("Metadata must be defined");
        }
        if (connectorDeployment.getMetadata().getResourceVersion() == null) {
            throw new IllegalArgumentException("Resource Version must be defined");
        }

        return connectorDeployment.getMetadata().getResourceVersion();
    }
}
