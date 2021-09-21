package org.bf2.cos.fleetshard.support.resources;

import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;

public final class Connectors {

    private Connectors() {
    }

    public static ManagedConnector newConnector(
        String clusterId,
        String connectorId,
        String deploymentId,
        Map<String, String> additionalLabels) {

        return new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(generateConnectorId(deploymentId))
                .addToLabels(LABEL_CLUSTER_ID, clusterId)
                .addToLabels(LABEL_CONNECTOR_ID, connectorId)
                .addToLabels(LABEL_DEPLOYMENT_ID, deploymentId)
                .addToLabels(additionalLabels)
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(clusterId)
                .withConnectorId(connectorId)
                .withDeploymentId(deploymentId)
                .build())
            .build();
    }

    public static String generateConnectorId(String deploymentId) {
        return Resources.CONNECTOR_PREFIX + deploymentId;
    }

}
