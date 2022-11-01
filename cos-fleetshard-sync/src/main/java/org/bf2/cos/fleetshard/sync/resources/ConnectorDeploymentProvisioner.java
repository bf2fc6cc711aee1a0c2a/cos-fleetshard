package org.bf2.cos.fleetshard.sync.resources;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleet.manager.model.SchemaRegistryConnectionSettings;
import org.bf2.cos.fleetshard.api.Conditions;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.SchemaRegistrySpec;
import org.bf2.cos.fleetshard.support.OperatorSelectorUtil;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.micrometer.core.instrument.Tags;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_ASSIGNED;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_TYPE;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_UOW;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class ConnectorDeploymentProvisioner {

    public static final String TAG_DEPLOYMENT_ID = "id";
    public static final String TAG_DEPLOYMENT_REVISION = "revision";
    public static final String METRICS_SUFFIX = "deployment.provision";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentProvisioner.class);

    @Inject
    FleetShardClient fleetShard;
    @Inject
    FleetManagerClient fleetManager;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    EventClient eventClient;

    @Inject
    @MetricsID(METRICS_SUFFIX)
    MetricsRecorder recorder;

    public void poll(long revision) {
        fleetManager.getDeployments(
            revision,
            this::provisionConnectors);
    }

    private void provisionConnectors(Collection<ConnectorDeployment> deployments) {
        for (ConnectorDeployment deployment : deployments) {
            this.recorder.record(
                () -> provision(deployment),
                Tags.of(TAG_DEPLOYMENT_ID, deployment.getId()),
                e -> {
                    LOGGER.error("Failure while trying to provision connector deployment: id={}, revision={}",
                        deployment.getId(),
                        deployment.getMetadata().getResourceVersion(),
                        e);

                    try {
                        MetaV1Condition condition = new MetaV1Condition();
                        condition.setType(Conditions.TYPE_READY);
                        condition.setStatus(Conditions.STATUS_FALSE);
                        condition.setReason(Conditions.FAILED_TO_CREATE_OR_UPDATE_RESOURCE_REASON);
                        condition.setMessage(e.getMessage());

                        ConnectorDeploymentStatus status = new ConnectorDeploymentStatus();
                        status.setResourceVersion(deployment.getMetadata().getResourceVersion());
                        status.addConditionsItem(condition);

                        fleetManager.updateConnectorStatus(
                            fleetShard.getClusterId(),
                            deployment.getId(),
                            status);
                    } catch (Exception ex) {
                        LOGGER.warn("Error wile reporting failure to the control plane", e);
                    }

                    fleetShard.getConnectorCluster().ifPresent(cc -> {
                        eventClient.broadcastWarning(
                            "FailedToCreateOrUpdateResource",
                            String.format("Unable to create or update deployment %s, revision: %s, reason: %s",
                                deployment.getId(),
                                deployment.getMetadata().getResourceVersion(),
                                e.getMessage()),
                            cc);
                    });
                });
        }
    }

    public void provision(ConnectorDeployment deployment) {
        final String uow = uid();

        LOGGER.info("Got cluster_id: {}, namespace_d: {}, connector_id: {}, deployment_id: {}, resource_version: {}, uow: {}",
            fleetShard.getClusterId(),
            deployment.getSpec().getNamespaceId(),
            deployment.getSpec().getConnectorId(),
            deployment.getId(),
            deployment.getMetadata().getResourceVersion(),
            uow);

        final ManagedConnector connector = createManagedConnector(uow, deployment, null);
        final Secret secret = createManagedConnectorSecret(uow, deployment, connector);

        LOGGER.info("CreateOrReplace - uow: {}, connector: {}/{}, secret: {}/{}",
            uow,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            secret.getMetadata().getNamespace(),
            secret.getMetadata().getName());
    }

    private ManagedConnector createManagedConnector(String uow, ConnectorDeployment deployment, HasMetadata owner) {

        ManagedConnector connector = fleetShard.getConnector(deployment).orElseGet(() -> {
            LOGGER.info(
                "Connector not found (cluster_id: {}, namespace_id: {}, connector_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                fleetShard.getClusterId(),
                deployment.getSpec().getNamespaceId(),
                deployment.getSpec().getConnectorId(),
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());

            ManagedConnector answer = new ManagedConnector();
            answer.setMetadata(new ObjectMeta());
            answer.getMetadata().setNamespace(fleetShard.generateNamespaceId(deployment.getSpec().getNamespaceId()));
            answer.getMetadata().setName(Connectors.generateConnectorId(deployment.getId()));

            Resources.setLabels(
                answer,
                LABEL_CLUSTER_ID, fleetShard.getClusterId(),
                LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId(),
                LABEL_DEPLOYMENT_ID, deployment.getId());

            answer.getSpec().setClusterId(fleetShard.getClusterId());
            answer.getSpec().setConnectorId(deployment.getSpec().getConnectorId());
            answer.getSpec().setDeploymentId(deployment.getId());

            return answer;
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
                Collection<Operator> operators = fleetShard.getOperators()
                    .stream()
                    .map(mco -> new Operator(
                        mco.getMetadata().getName(),
                        mco.getSpec().getType(),
                        mco.getSpec().getVersion()))
                    .collect(Collectors.toList());

                OperatorSelectorUtil.assign(operatorSelector, operators)
                    .map(Operator::getId)
                    .ifPresent(operatorSelector::setId);
            }
        }
        if (operatorSelector.getId() != null) {
            Resources.setLabel(
                connector,
                LABEL_OPERATOR_ASSIGNED,
                operatorSelector.getId());
        }
        if (operatorSelector.getType() != null) {
            Resources.setLabel(
                connector,
                LABEL_OPERATOR_TYPE,
                operatorSelector.getType());
        }

        if (config != null) {
            config.connectors().labels().forEach((k, v) -> {
                Resources.setLabel(connector, k, v);
            });
            config.connectors().annotations().forEach((k, v) -> {
                Resources.setAnnotation(connector, k, v);
            });
        }

        Resources.setOwnerReferences(
            connector,
            owner);

        // add resource version to label
        Resources.setLabel(
            connector,
            LABEL_DEPLOYMENT_RESOURCE_VERSION,
            "" + deployment.getMetadata().getResourceVersion());

        // add uow
        Resources.setLabel(
            connector,
            LABEL_UOW,
            uow);

        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState().getValue());
        connector.getSpec().getDeployment().setConnectorTypeId(deployment.getSpec().getConnectorTypeId());
        connector.getSpec().getDeployment().setConnectorResourceVersion(deployment.getSpec().getConnectorResourceVersion());

        KafkaConnectionSettings kafkaConnectionSettings = deployment.getSpec().getKafka();
        if (kafkaConnectionSettings != null) {
            connector.getSpec().getDeployment().setKafka(new KafkaSpec(
                kafkaConnectionSettings.getId(),
                kafkaConnectionSettings.getUrl()));
        }

        SchemaRegistryConnectionSettings schemaRegistryConnectionSettings = deployment.getSpec().getSchemaRegistry();
        if (schemaRegistryConnectionSettings != null) {
            connector.getSpec().getDeployment().setSchemaRegistry(new SchemaRegistrySpec(
                schemaRegistryConnectionSettings.getId(),
                schemaRegistryConnectionSettings.getUrl()));
        }

        connector.getSpec().getDeployment().setConnectorResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setSecret(Secrets.generateConnectorSecretId(deployment.getId()));
        connector.getSpec().getDeployment().setUnitOfWork(uow);
        connector.getSpec().setOperatorSelector(operatorSelector);

        LOGGER.info("Provisioning connector namespace: {}, name: {}, revision: {}",
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            connector.getSpec().getDeployment().getDeploymentResourceVersion());

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
                    "Secret not found (cluster_id: {}, namespace_id: {}, connector_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                    fleetShard.getClusterId(),
                    deployment.getSpec().getNamespaceId(),
                    deployment.getSpec().getConnectorId(),
                    deployment.getId(),
                    deployment.getMetadata().getResourceVersion());

                Secret answer = new Secret();
                answer.setMetadata(new ObjectMeta());
                answer.getMetadata().setNamespace(fleetShard.generateNamespaceId(deployment.getSpec().getNamespaceId()));
                answer.getMetadata().setName(Secrets.generateConnectorSecretId(deployment.getId()));

                Resources.setLabels(
                    answer,
                    LABEL_CLUSTER_ID, fleetShard.getClusterId(),
                    LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId(),
                    LABEL_DEPLOYMENT_ID, deployment.getId(),
                    LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + deployment.getMetadata().getResourceVersion());

                return answer;
            });

        Resources.setOwnerReferences(
            secret,
            owner);

        // add resource version to label
        Resources.setLabel(
            secret,
            LABEL_DEPLOYMENT_RESOURCE_VERSION,
            "" + deployment.getMetadata().getResourceVersion());

        // add uow
        Resources.setLabel(
            secret,
            LABEL_UOW,
            uow);

        // copy operator type
        Resources.setLabel(
            secret,
            LABEL_OPERATOR_TYPE,
            owner.getMetadata().getLabels().get(LABEL_OPERATOR_TYPE));

        Secrets.set(secret, Secrets.SECRET_ENTRY_CONNECTOR, deployment.getSpec().getConnectorSpec());
        Secrets.set(secret, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, deployment.getSpec().getServiceAccount());
        Secrets.set(secret, Secrets.SECRET_ENTRY_META, deployment.getSpec().getShardMetadata());

        try {
            return fleetShard.createSecret(secret);
        } catch (Exception e) {
            LOGGER.warn("", e);
            throw e;
        }
    }
}
