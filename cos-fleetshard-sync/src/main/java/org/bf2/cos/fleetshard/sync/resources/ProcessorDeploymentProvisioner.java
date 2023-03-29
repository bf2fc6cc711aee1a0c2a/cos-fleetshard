package org.bf2.cos.fleetshard.sync.resources;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleet.manager.model.ProcessorDeployment;
import org.bf2.cos.fleet.manager.model.ProcessorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.SchemaRegistryConnectionSettings;
import org.bf2.cos.fleetshard.api.Conditions;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.Processor;
import org.bf2.cos.fleetshard.api.SchemaRegistrySpec;
import org.bf2.cos.fleetshard.support.OperatorSelectorUtil;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Processors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.metrics.MetricsID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_ASSIGNED;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_TYPE;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_PROCESSOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_UOW;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class ProcessorDeploymentProvisioner {

    public static final String TAG_DEPLOYMENT_ID = "id";
    public static final String TAG_DEPLOYMENT_REVISION = "revision";
    public static final String METRICS_SUFFIX = "deployment.provision";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorDeploymentProvisioner.class);

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
        fleetManager.getProcessorDeployments(
            revision,
            this::provisionProcessor);
    }

    private void provisionProcessor(Collection<ProcessorDeployment> deployments) {
        for (ProcessorDeployment deployment : deployments) {
            this.recorder.record(
                () -> provision(deployment),
                e -> {
                    LOGGER.error("Failure while trying to provision processor deployment: id={}, revision={}",
                        deployment.getId(),
                        deployment.getMetadata().getResourceVersion(),
                        e);

                    try {
                        MetaV1Condition condition = new MetaV1Condition();
                        condition.setType(Conditions.TYPE_READY);
                        condition.setStatus(Conditions.STATUS_FALSE);
                        condition.setReason(Conditions.FAILED_TO_CREATE_OR_UPDATE_RESOURCE_REASON);
                        condition.setMessage(e.getMessage());

                        ProcessorDeploymentStatus status = new ProcessorDeploymentStatus();
                        status.setResourceVersion(deployment.getMetadata().getResourceVersion());
                        status.addConditionsItem(condition);

                        fleetManager.updateProcessorStatus(
                            fleetShard.getClusterId(),
                            deployment.getId(),
                            status);
                    } catch (Exception ex) {
                        LOGGER.warn("Error wile reporting failure to the control plane", e);
                    }

                    fleetShard.getConnectorCluster().ifPresent(cc -> {
                        eventClient.broadcastWarning(
                            "FailedToCreateOrUpdateResource",
                            String.format("Unable to create or update processor deployment %s, revision: %s, reason: %s",
                                deployment.getId(),
                                deployment.getMetadata().getResourceVersion(),
                                e.getMessage()),
                            cc);
                    });
                });
        }
    }

    public void provision(ProcessorDeployment deployment) {
        final String uow = uid();

        LOGGER.info("Got cluster_id: {}, namespace_d: {}, processor_id: {}, deployment_id: {}, resource_version: {}, uow: {}",
            fleetShard.getClusterId(),
            deployment.getSpec().getNamespaceId(),
            deployment.getSpec().getProcessorId(),
            deployment.getId(),
            deployment.getMetadata().getResourceVersion(),
            uow);

        final Processor processor = createProcessor(uow, deployment, null);
        final Secret secret = createProcessorSecret(uow, deployment, processor);

        LOGGER.info("CreateOrReplace - uow: {}, processor: {}/{}, secret: {}/{}",
            uow,
            processor.getMetadata().getNamespace(),
            processor.getMetadata().getName(),
            secret.getMetadata().getNamespace(),
            secret.getMetadata().getName());
    }

    private Processor createProcessor(String uow, ProcessorDeployment deployment, HasMetadata owner) {

        Processor processor = fleetShard.getProcessor(deployment).orElseGet(() -> {
            LOGGER.info(
                "Processor not found (cluster_id: {}, namespace_id: {}, processor_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                fleetShard.getClusterId(),
                deployment.getSpec().getNamespaceId(),
                deployment.getSpec().getProcessorId(),
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());

            Processor answer = new Processor();
            answer.setMetadata(new ObjectMeta());
            answer.getMetadata().setNamespace(fleetShard.generateNamespaceId(deployment.getSpec().getNamespaceId()));
            answer.getMetadata().setName(Processors.generateProcessorId(deployment.getId()));

            Resources.setLabels(
                answer,
                LABEL_CLUSTER_ID, fleetShard.getClusterId(),
                LABEL_PROCESSOR_ID, deployment.getSpec().getProcessorId(),
                LABEL_DEPLOYMENT_ID, deployment.getId());

            answer.getSpec().setClusterId(fleetShard.getClusterId());
            answer.getSpec().setProcessorId(deployment.getSpec().getProcessorId());
            answer.getSpec().setDeploymentId(deployment.getId());

            return answer;
        });

        // TODO: change APIs to include a single operator
        // move operator one level up
        // include full operator info in ProcessorDeployment APIs
        ArrayNode operatorsMeta = deployment.getSpec().getShardMetadata().withArray("operators");
        if (operatorsMeta.size() != 1) {
            throw new IllegalArgumentException("Multiple selectors are not yet supported");
        }

        OperatorSelector operatorSelector = new OperatorSelector(
            deployment.getSpec().getOperatorId(),
            operatorsMeta.get(0).requiredAt("/type").asText(),
            operatorsMeta.get(0).requiredAt("/version").asText());

        if (operatorSelector.getId() == null) {
            final OperatorSelector currentSelector = processor.getSpec().getOperatorSelector();

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
                    .ifPresentOrElse(
                        operatorSelector::setId,
                        () -> {
                            eventClient.broadcastWarning(
                                "NoAssignableOperator",
                                String.format("Unable to find a supported operator for deployment_id: %s", deployment.getId()),
                                processor);
                        });
            }
        }
        if (operatorSelector.getId() != null) {
            Resources.setLabel(
                processor,
                LABEL_OPERATOR_ASSIGNED,
                operatorSelector.getId());
        }
        if (operatorSelector.getType() != null) {
            Resources.setLabel(
                processor,
                LABEL_OPERATOR_TYPE,
                operatorSelector.getType());
        }

        if (config != null) {
            config.processors().labels().forEach((k, v) -> {
                Resources.setLabel(processor, k, v);
            });
            config.processors().annotations().forEach((k, v) -> {
                Resources.setAnnotation(processor, k, v);
            });
        }

        Resources.setOwnerReferences(
            processor,
            owner);

        // add resource version to label
        Resources.setLabel(
            processor,
            LABEL_DEPLOYMENT_RESOURCE_VERSION,
            "" + deployment.getMetadata().getResourceVersion());

        // add uow
        Resources.setLabel(
            processor,
            LABEL_UOW,
            uow);

        processor.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        processor.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState().getValue());
        processor.getSpec().getDeployment().setProcessorTypeId(deployment.getSpec().getProcessorTypeId());
        processor.getSpec().getDeployment().setProcessorResourceVersion(deployment.getSpec().getProcessorResourceVersion());

        KafkaConnectionSettings kafkaConnectionSettings = deployment.getSpec().getKafka();
        if (kafkaConnectionSettings != null) {
            processor.getSpec().getDeployment().setKafka(new KafkaSpec(
                kafkaConnectionSettings.getId(),
                kafkaConnectionSettings.getUrl()));
        }

        SchemaRegistryConnectionSettings schemaRegistryConnectionSettings = deployment.getSpec().getSchemaRegistry();
        if (schemaRegistryConnectionSettings != null) {
            processor.getSpec().getDeployment().setSchemaRegistry(new SchemaRegistrySpec(
                schemaRegistryConnectionSettings.getId(),
                schemaRegistryConnectionSettings.getUrl()));
        }

        processor.getSpec().getDeployment().setProcessorResourceVersion(deployment.getSpec().getProcessorResourceVersion());
        processor.getSpec().getDeployment().setSecret(Secrets.generateProcessorSecretId(deployment.getId()));
        processor.getSpec().getDeployment().setUnitOfWork(uow);
        processor.getSpec().setOperatorSelector(operatorSelector);

        copyMetadata(deployment, processor);

        LOGGER.info("Provisioning processor namespace: {}, name: {}, revision: {}",
            processor.getMetadata().getNamespace(),
            processor.getMetadata().getName(),
            processor.getSpec().getDeployment().getDeploymentResourceVersion());

        try {
            return fleetShard.createProcessor(processor);
        } catch (Exception e) {
            LOGGER.warn("", e);
            throw e;
        }
    }

    private Secret createProcessorSecret(String uow, ProcessorDeployment deployment, Processor owner) {
        Secret secret = fleetShard.getSecret(deployment)
            .orElseGet(() -> {
                LOGGER.info(
                    "Secret not found (cluster_id: {}, namespace_id: {}, processor_id: {}, deployment_id: {}, resource_version: {}), creating a new one",
                    fleetShard.getClusterId(),
                    deployment.getSpec().getNamespaceId(),
                    deployment.getSpec().getProcessorId(),
                    deployment.getId(),
                    deployment.getMetadata().getResourceVersion());

                Secret answer = new Secret();
                answer.setMetadata(new ObjectMeta());
                answer.getMetadata().setNamespace(fleetShard.generateNamespaceId(deployment.getSpec().getNamespaceId()));
                answer.getMetadata().setName(Secrets.generateProcessorSecretId(deployment.getId()));

                Resources.setLabels(
                    answer,
                    LABEL_CLUSTER_ID, fleetShard.getClusterId(),
                    LABEL_PROCESSOR_ID, deployment.getSpec().getProcessorId(),
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

        Secrets.set(secret, Secrets.SECRET_ENTRY_PROCESSOR, deployment.getSpec().getProcessorSpec());
        Secrets.set(secret, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, deployment.getSpec().getServiceAccount());
        Secrets.set(secret, Secrets.SECRET_ENTRY_META, deployment.getSpec().getShardMetadata());

        copyMetadata(deployment, secret);

        try {
            return fleetShard.createSecret(secret);
        } catch (Exception e) {
            LOGGER.warn("", e);
            throw e;
        }
    }

    // TODO remove duplication here
    private void copyMetadata(ProcessorDeployment deployment, HasMetadata target) {
        if (deployment.getMetadata() != null && deployment.getMetadata().getAnnotations() != null) {
            config.metrics().recorder().tags().labels()
                .stream()
                .flatMap(List::stream)
                .forEach(key -> {
                    String val = deployment.getMetadata().getAnnotations().get(key);
                    if (val != null) {
                        Resources.setLabel(target, key, val);
                    }
                });

            config.metrics().recorder().tags().annotations()
                .stream()
                .flatMap(List::stream)
                .forEach(key -> {
                    String val = deployment.getMetadata().getAnnotations().get(key);
                    if (val != null) {
                        Resources.setAnnotation(target, key, val);
                    }
                });
        }
    }
}
