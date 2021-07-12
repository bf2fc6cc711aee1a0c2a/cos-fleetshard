package org.bf2.cos.fleetshard.operator.connector;

import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.support.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentProvisioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentProvisioner.class);

    @Inject
    FleetShardClient fleetShard;
    @Inject
    KubernetesClient kubernetesClient;

    public void provision(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        LOGGER.info("Got connector_id: {}, deployment_id: {}",
            deployment.getSpec().getConnectorId(),
            deployment.getId());

        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = connectorCluster.getSpec().getConnectorsNamespace();
        final String mcId = "c" + UUID.randomUUID().toString().replaceAll("-", "");

        ManagedConnector connector = fleetShard.lookupManagedConnector(connectorCluster, deployment).orElseGet(() -> {
            LOGGER.info("Connector not found (connector_id: {}, deployment_id: {}), creating a new one",
                deployment.getSpec().getConnectorId(),
                deployment.getId());

            // TODO: find suitable operator and label according
            return new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(mcId)
                    .withNamespace(connectorsNs)
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
                    .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withClusterId(connectorCluster.getSpec().getId())
                    .withConnectorId(connectorId)
                    .withConnectorTypeId(deployment.getSpec().getConnectorTypeId())
                    .withDeploymentId(deployment.getId())
                    .build())
                .build();
        });

        final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
        final Long drv = deployment.getMetadata().getResourceVersion();

        if (Objects.equals(cdrv, drv)) {
            LOGGER.info(
                "Skipping as deployment resource version has not changed (deployment_id={}, version={})",
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());
            return;
        }

        ArrayNode operatorsMeta = deployment.getSpec().getShardMetadata().withArray("operators");
        if (operatorsMeta.size() != 1) {
            throw new IllegalArgumentException("Multiple selectors are not yet supported");
        }

        OperatorSelector operatorSelector = new OperatorSelector(
            deployment.getSpec().getOperatorId(),
            operatorsMeta.get(0).requiredAt("/type").asText(),
            operatorsMeta.get(0).requiredAt("/version").asText());

        connector.getSpec().getDeployment().setResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState());
        connector.getSpec().setOperatorSelector(operatorSelector);

        LOGGER.info("provisioning connector id={} rv={} - {}/{}: {}",
            mcId,
            deployment.getMetadata().getResourceVersion(),
            connectorsNs,
            connectorId,
            Serialization.asJson(deployment));

        kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNs)
            .createOrReplace(connector);
    }
}
