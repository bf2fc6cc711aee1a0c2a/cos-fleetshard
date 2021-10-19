package org.bf2.cos.fleetshard.sync.connector;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.support.OperatorSelectorUtil;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class ConnectorDeploymentProvisioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentProvisioner.class);

    @Inject
    FleetShardSyncConfig config;

    private final FleetShardClient fleetShard;

    public ConnectorDeploymentProvisioner(FleetShardClient connectorClient) {
        this.fleetShard = connectorClient;
    }

    public void provision(ConnectorDeployment deployment) {
        final String uow = uid();

        LOGGER.info("Got cluster_id: {}, connector_id: {}, deployment_id: {}, resource_version: {}, uow: {}",
            fleetShard.getClusterId(),
            deployment.getSpec().getConnectorId(),
            deployment.getId(),
            deployment.getMetadata().getResourceVersion(),
            uow);

        // TODO: cache cluster
        final ManagedConnectorCluster cluster = fleetShard.getOrCreateManagedConnectorCluster();
        final ManagedConnector connector = createManagedConnector(uow, deployment, cluster);
        final Secret secret = createManagedConnectorSecret(uow, deployment, connector);

        LOGGER.info("CreateOrReplace - uow: {}, managed_connector: {}/{}, managed_connector_secret: {}/{}",
            uow,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            secret.getMetadata().getNamespace(),
            secret.getMetadata().getName());
    }

    private ManagedConnector createManagedConnector(String uow, ConnectorDeployment deployment, HasMetadata owner) {
        ManagedConnector connector = fleetShard.getConnector(deployment).orElseGet(() -> {
            LOGGER.info(
                "Connector not found (cluster_id: {}, connector_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                fleetShard.getClusterId(),
                deployment.getSpec().getConnectorId(),
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());

            return Connectors.newConnector(
                fleetShard.getClusterId(),
                deployment.getSpec().getConnectorId(),
                deployment.getId(),
                Map.of());
        });

        // TODO: change APIs to include a single operator
        // move operator one level up
        // include full operator info in ConnectorDeployment APIs
        ArrayNode operatorsMeta = deployment.getSpec().getShardMetadata().withArray("operators");
        if (operatorsMeta.size() != 1) {
            throw new IllegalArgumentException("Multiple selectors are not yet supported");
        }

        OperatorSelector operatorSelector = new OperatorSelector(
            deployment.getSpec().getOperatorId(),
            operatorsMeta.get(0).requiredAt("/type").asText(),
            operatorsMeta.get(0).requiredAt("/version").asText());

        if (operatorSelector.getId() == null) {
            final OperatorSelector currentSelector = connector.getSpec().getOperatorSelector();

            // don't select a new operator if previously set.
            if (currentSelector != null && currentSelector.getId() != null) {
                operatorSelector.setId(currentSelector.getId());
            } else {
                OperatorSelectorUtil.assign(operatorSelector, fleetShard.lookupOperators())
                    .map(Operator::getId)
                    .ifPresent(operatorSelector::setId);
            }
        }
        if (operatorSelector.getId() != null) {
            Resources.setLabel(
                connector,
                Resources.LABEL_OPERATOR_ASSIGNED,
                operatorSelector.getId());
        }
        if (operatorSelector.getType() != null) {
            Resources.setLabel(
                connector,
                Resources.LABEL_OPERATOR_TYPE,
                operatorSelector.getType());
        }

        if (config != null) {
            config.connectors().kcp().clusterId().ifPresent(clusterId -> {
                Resources.setLabel(
                    connector,
                    Resources.LABEL_KCP_TARGET_CLUSTER_ID,
                    clusterId);
            });
        }

        connector.getMetadata().setOwnerReferences(List.of(
            new OwnerReferenceBuilder()
                .withApiVersion(owner.getApiVersion())
                .withKind(owner.getKind())
                .withName(owner.getMetadata().getName())
                .withUid(owner.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .build()));

        // add resource version to label
        Resources.setLabel(
            connector,
            Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION,
            "" + deployment.getMetadata().getResourceVersion());

        // add uow
        Resources.setLabel(
            connector,
            Resources.LABEL_UOW,
            uow);

        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState());
        connector.getSpec().getDeployment().setConnectorTypeId(deployment.getSpec().getConnectorTypeId());
        connector.getSpec().getDeployment().setConnectorResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setSecret(Secrets.generateConnectorSecretId(deployment.getId()));
        connector.getSpec().getDeployment().setUnitOfWork(uow);
        connector.getSpec().setOperatorSelector(operatorSelector);

        LOGGER.info("Provisioning connector id={} rv={} - {}/{}: {}",
            connector.getMetadata().getName(),
            connector.getSpec().getDeployment().getDeploymentResourceVersion(),
            fleetShard.getConnectorsNamespace(),
            connector.getSpec().getConnectorId(),
            Serialization.asJson(connector.getSpec()));

        try {
            return fleetShard.createConnector(connector);
        } catch (Exception e) {
            LOGGER.warn("", e);
            throw e;
        }
    }

    private Secret createManagedConnectorSecret(String uow, ConnectorDeployment deployment, ManagedConnector owner) {
        Secret secret = fleetShard.getSecret(deployment)
            .orElseGet(() -> {
                LOGGER.info(
                    "Secret not found (cluster_id: {}, connector_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                    fleetShard.getClusterId(),
                    deployment.getSpec().getConnectorId(),
                    deployment.getId(),
                    deployment.getMetadata().getResourceVersion());

                return Secrets.newSecret(
                    Secrets.generateConnectorSecretId(deployment.getId()),
                    fleetShard.getClusterId(),
                    deployment.getSpec().getConnectorId(),
                    deployment.getId(),
                    deployment.getMetadata().getResourceVersion(),
                    Map.of());
            });

        secret.getMetadata().setOwnerReferences(List.of(
            new OwnerReferenceBuilder()
                .withApiVersion(owner.getApiVersion())
                .withKind(owner.getKind())
                .withName(owner.getMetadata().getName())
                .withUid(owner.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .build()));

        // add resource version to label
        Resources.setLabel(
            secret,
            Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION,
            "" + deployment.getMetadata().getResourceVersion());

        // add uow
        Resources.setLabel(
            secret,
            Resources.LABEL_UOW,
            uow);

        // copy operator type
        Resources.setLabel(
            secret,
            Resources.LABEL_OPERATOR_TYPE,
            owner.getMetadata().getLabels().get(Resources.LABEL_OPERATOR_TYPE));

        Secrets.set(secret, Secrets.SECRET_ENTRY_CONNECTOR, deployment.getSpec().getConnectorSpec());
        Secrets.set(secret, Secrets.SECRET_ENTRY_KAFKA, deployment.getSpec().getKafka());
        Secrets.set(secret, Secrets.SECRET_ENTRY_META, deployment.getSpec().getShardMetadata());

        try {
            return fleetShard.createSecret(secret);
        } catch (Exception e) {
            LOGGER.warn("", e);
            throw e;
        }
    }
}
