package org.bf2.cos.fleetshard.operator.connector;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.api.ManagedConnector.CONTEXT_OPERAND;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_OPERATOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_TYPE_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_RESOURCE_CONTEXT;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_WATCH;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_FAILED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;
import static org.bf2.cos.fleetshard.support.OperatorSelectorUtil.available;
import static org.bf2.cos.fleetshard.support.resources.Resources.getDeletionMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorConditions;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorBuilder;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEventSource;
import org.bf2.cos.fleetshard.operator.operand.OperandController;
import org.bf2.cos.fleetshard.operator.operand.OperandResourceWatcher;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.zjsonpatch.JsonDiff;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller(name = "connector", finalizerName = Controller.NO_FINALIZER, generationAwareEventProcessing = false)
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    @Inject
    ManagedConnectorOperator managedConnectorOperator;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    OperandController operandController;

    @ConfigProperty(name = "cos.connectors.watch.resources", defaultValue = "true")
    boolean watchResource;

    @Override
    public void registerEventSources(EventSourceManager eventSourceManager) {
        if (watchResource) {
            eventSourceManager.registerEventSource(
                "_operators",
                new ConnectorOperatorEventSource(kubernetesClient, fleetShard.getOperatorNamespace()));
            eventSourceManager.registerEventSource(
                "_secrets",
                new OperandResourceWatcher(kubernetesClient, "v1", "Secret", fleetShard.getConnectorsNamespace()));

            for (ResourceDefinitionContext res : operandController.getResourceTypes()) {
                if ("v1".equals(res.getVersion()) && "Secret".equals(res.getKind())) {
                    continue;
                }

                eventSourceManager.registerEventSource(
                    "_" + res.getGroup() + "/" + res.getVersion() + ":" + res.getKind(),
                    new OperandResourceWatcher(kubernetesClient, res, fleetShard.getConnectorsNamespace()));
            }
        }
    }

    @Override
    public UpdateControl<ManagedConnector> createOrUpdateResource(
        ManagedConnector connector,
        Context<ManagedConnector> context) {

        boolean canHandle = Objects.equals(
            managedConnectorOperator.getMetadata().getName(),
            connector.getSpec().getOperatorSelector().getId());

        if (!canHandle) {
            LOGGER.debug("Skip connector: {} as assigned to operator: {}",
                connector.getMetadata().getName(),
                connector.getSpec().getOperatorSelector().getId());

            return UpdateControl.noUpdate();
        }

        LOGGER.info("Reconcile {}:{}:{} (phase={})",
            connector.getApiVersion(),
            connector.getKind(),
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        if (connector.getStatus().getPhase() == null) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
            connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
            return UpdateControl.updateStatusSubResource(connector);
        }

        switch (connector.getStatus().getPhase()) {
            case Initialization:
                return handleInitialization(connector);
            case Augmentation:
                return handleAugmentation(connector);
            case Monitor:
                return handleMonitor(connector);
            case Deleting:
                return handleDeleting(connector);
            case Deleted:
                return handleDeleted(connector);
            case Stopping:
                return handleStopping(connector);
            case Stopped:
                return handleStopped(connector);
            case Error:
                return handleError(connector);
            default:
                throw new UnsupportedOperationException("Unsupported phase: " + connector.getStatus().getPhase());
        }
    }

    // **************************************************
    //
    // Handlers
    //
    // **************************************************

    private UpdateControl<ManagedConnector> handleInitialization(ManagedConnector connector) {
        ManagedConnectorConditions.clearConditions(connector);
        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Initialization,
            ManagedConnectorConditions.Status.True,
            "Initialization",
            "Initialization");
        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.False,
            "Initialization",
            "Initialization");

        switch (connector.getSpec().getDeployment().getDesiredState()) {
            case DESIRED_STATE_DELETED: {
                connector.getStatus().setDeployment(connector.getSpec().getDeployment());
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Deleting);
                connector.getStatus().getConnectorStatus().setPhase(STATE_DE_PROVISIONING);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
                break;
            }
            case DESIRED_STATE_STOPPED: {
                connector.getStatus().setDeployment(connector.getSpec().getDeployment());
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Stopping);
                connector.getStatus().getConnectorStatus().setPhase(STATE_DE_PROVISIONING);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
                break;
            }
            case DESIRED_STATE_READY: {
                connector.getStatus().getConnectorStatus().setAssignedOperator(
                    new OperatorBuilder()
                        .withType(managedConnectorOperator.getSpec().getType())
                        .withId(managedConnectorOperator.getMetadata().getName())
                        .withVersion(managedConnectorOperator.getSpec().getVersion())
                        .build());

                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Augmentation);
                connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
                break;
            }
            default:
                throw new IllegalStateException(
                    "Unknown desired state: " + connector.getSpec().getDeployment().getDesiredState());
        }

        return UpdateControl.updateStatusSubResource(connector);
    }

    private UpdateControl<ManagedConnector> handleAugmentation(ManagedConnector connector) {
        final DeploymentSpec ref = connector.getSpec().getDeployment();

        if (connector.getSpec().getDeployment().getSecret() == null) {
            LOGGER.info("Secret {} not found, retry in 1s", connector.getSpec().getDeployment().getSecret());
            getRetryTimer().scheduleOnce(connector, 1000);

            return UpdateControl.noUpdate();
        }

        Secret secret = kubernetesClient.secrets()
            .inNamespace(fleetShard.getConnectorsNamespace())
            .withName(connector.getSpec().getDeployment().getSecret())
            .get();

        if (secret == null) {
            ManagedConnectorConditions.setCondition(
                connector,
                ManagedConnectorConditions.Type.Augmentation,
                ManagedConnectorConditions.Status.False,
                "SecretNotFound",
                "Unable to find secret with name: " + connector.getSpec().getDeployment().getSecret());
            ManagedConnectorConditions.setCondition(
                connector,
                ManagedConnectorConditions.Type.Ready,
                ManagedConnectorConditions.Status.False,
                "AugmentationError",
                "AugmentationError");

            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Error);
            connector.getStatus().getConnectorStatus().setPhase(STATE_FAILED);

            return UpdateControl.updateStatusSubResource(connector);
        }

        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Augmentation,
            ManagedConnectorConditions.Status.True,
            "Augmentation",
            "Augmentation");
        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.False,
            "Augmentation",
            "Augmentation");

        for (var resource : operandController.reify(connector, secret)) {
            if (resource.getMetadata().getLabels() == null) {
                resource.getMetadata().setLabels(new HashMap<>());
            }
            if (resource.getMetadata().getAnnotations() == null) {
                resource.getMetadata().setAnnotations(new HashMap<>());
            }

            final String rv = Long.toString(connector.getSpec().getDeployment().getDeploymentResourceVersion());
            final String deletionMode = getDeletionMode(resource).orElse(DELETION_MODE_CONNECTOR);

            final Map<String, String> labels = KubernetesResourceUtil.getOrCreateLabels(resource);
            labels.put(LABEL_WATCH, "true");
            labels.put(LABEL_RESOURCE_CONTEXT, CONTEXT_OPERAND);
            labels.put(LABEL_CONNECTOR_OPERATOR, connector.getStatus().getConnectorStatus().getAssignedOperator().getId());
            labels.put(LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId());
            labels.put(LABEL_CONNECTOR_TYPE_ID, connector.getSpec().getDeployment().getConnectorTypeId());
            labels.put(LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId());
            labels.put(LABEL_CLUSTER_ID, connector.getSpec().getClusterId());

            final Map<String, String> annotations = KubernetesResourceUtil.getOrCreateAnnotations(resource);
            annotations.put(ANNOTATION_DEPLOYMENT_RESOURCE_VERSION, rv);

            resource.getMetadata().setOwnerReferences(List.of(
                new OwnerReferenceBuilder()
                    .withApiVersion(connector.getApiVersion())
                    .withKind(connector.getKind())
                    .withName(connector.getMetadata().getName())
                    .withUid(connector.getMetadata().getUid())
                    .withBlockOwnerDeletion(true)
                    .build()));

            final DeployedResource res = DeployedResource.of(
                uc.createOrReplace(
                    connector.getMetadata().getNamespace(),
                    resource));

            connector.getStatus().getConnectorStatus().addOrUpdateResource(res);

            if (!DELETION_MODE_CONNECTOR.equals(deletionMode)) {
                res.setDeploymentRevision(ref.getDeploymentResourceVersion());
            }
        }

        // Add the secret created by the sync among the list of resources to
        // clean-up upon delete/update
        connector.getStatus().getConnectorStatus().addOrUpdateResource(DeployedResource.of(secret));

        connector.getStatus().setDeployment(connector.getSpec().getDeployment());
        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);
        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

        return UpdateControl.updateStatusSubResource(connector);
    }

    private UpdateControl<ManagedConnector> handleMonitor(ManagedConnector connector) {
        if (!Objects.equals(connector.getSpec().getDeployment(), connector.getStatus().getDeployment())) {
            JsonNode specNode = Serialization.jsonMapper().valueToTree(connector.getSpec().getDeployment());
            JsonNode statusNode = Serialization.jsonMapper().valueToTree(connector.getStatus().getDeployment());

            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
            connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Drift detected {}, move to phase: {}",
                JsonDiff.asJson(statusNode, specNode),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        if (connector.getStatus().getConnectorStatus() != null
            && connector.getStatus().getConnectorStatus().getResources() != null
            && connector.getStatus().getDeployment().getDeploymentResourceVersion() != null) {

            final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();

            for (DeployedResource dr : connector.getStatus().getConnectorStatus().getResources()) {
                if (dr.getDeploymentRevision() != null && !Objects.equals(cdrv, dr.getDeploymentRevision())) {
                    continue;
                }
                if (dr.getGeneration() == null && dr.getResourceVersion() == null) {
                    continue;
                }

                final GenericKubernetesResource res = uc.get(dr);
                if (res == null) {
                    continue;
                }

                if (dr.getGeneration() != null) {
                    // custom resource
                    if (!Objects.equals(dr.getGeneration(), res.getMetadata().getGeneration())) {
                        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
                        connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                        LOGGER.info(
                            "Deployed resource drift detected observed_generation: {}, resource_generation: {}, move to phase: {}",
                            dr.getGeneration(),
                            res.getMetadata().getGeneration(),
                            connector.getStatus().getPhase());

                        return UpdateControl.updateStatusSubResource(connector);
                    }
                } else if (dr.getResourceVersion() != null) {
                    // secret, configmap etc
                    if (!Objects.equals(dr.getResourceVersion(), res.getMetadata().getResourceVersion())) {
                        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
                        connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                        LOGGER.info(
                            "Deployed resource drift detected observed_resource_version: {}, resource_version: {}, move to phase: {}",
                            dr.getResourceVersion(),
                            res.getMetadata().getResourceVersion(),
                            connector.getStatus().getPhase());

                        return UpdateControl.updateStatusSubResource(connector);
                    }
                }
            }
        }

        operandController.status(connector);
        operandController.gc(connector);

        //
        // Search for newly installed ManagedOperators
        //
        final List<Operator> operators = fleetShard.lookupOperators();
        final Operator assignedOperator = connector.getStatus().getConnectorStatus().getAssignedOperator();
        final Operator availableOperator = connector.getStatus().getConnectorStatus().getAvailableOperator();
        final OperatorSelector selector = connector.getSpec().getOperatorSelector();

        var maybeAvailable = available(selector, operators)
            .filter(operator -> !Objects.equals(operator, assignedOperator) && !Objects.equals(operator, availableOperator));

        if (maybeAvailable.isPresent()) {
            LOGGER.info("deployment (upd): {} -> from:{}, to: {}",
                connector.getSpec().getDeployment(),
                assignedOperator,
                maybeAvailable.get());

            connector.getStatus().getConnectorStatus().setAvailableOperator(maybeAvailable.get());
        }

        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Monitor,
            ManagedConnectorConditions.Status.True,
            "Monitor",
            "Monitor");
        ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.True,
            "Ready",
            "Ready");

        return UpdateControl.updateStatusSubResource(connector);
    }

    private UpdateControl<ManagedConnector> handleDeleting(ManagedConnector connector) {
        if (operandController.delete(connector)) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Deleted);
            connector.getStatus().getConnectorStatus().setPhase(STATE_DELETED);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Connector {} deleted, move to phase: {}",
                connector.getMetadata().getName(),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        // TODO: reschedule a cleanup with backoff
        getRetryTimer().scheduleOnce(connector, 1500);

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleDeleted(ManagedConnector connector) {
        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleStopping(ManagedConnector connector) {
        if (operandController.stop(connector)) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Stopped);
            connector.getStatus().getConnectorStatus().setPhase(STATE_STOPPED);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Connector {} stopped, move to phase: {}",
                connector.getMetadata().getName(),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        // TODO: reschedule a cleanup with backoff
        getRetryTimer().scheduleOnce(connector, 1500);

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleStopped(ManagedConnector connector) {
        boolean updated = !Objects.equals(
            connector.getSpec().getDeployment(),
            connector.getStatus().getDeployment());

        if (updated) {
            JsonNode specNode = Serialization.jsonMapper().valueToTree(connector.getSpec().getDeployment());
            JsonNode statusNode = Serialization.jsonMapper().valueToTree(connector.getStatus().getDeployment());

            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
            connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Drift detected {}, move to phase: {}",
                JsonDiff.asJson(statusNode, specNode),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        if (connector.getStatus().getConnectorStatus() != null
            && connector.getStatus().getConnectorStatus().getResources() != null
            && connector.getStatus().getDeployment().getDeploymentResourceVersion() != null) {

            final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();

            for (DeployedResource dr : connector.getStatus().getConnectorStatus().getResources()) {
                if (dr.getDeploymentRevision() != null && !Objects.equals(cdrv, dr.getDeploymentRevision())) {
                    continue;
                }
                if (dr.getGeneration() == null && dr.getResourceVersion() == null) {
                    continue;
                }

                final GenericKubernetesResource res = uc.get(dr);
                if (res == null) {
                    continue;
                }

                if (dr.getGeneration() != null) {
                    if (!Objects.equals(dr.getGeneration(), res.getMetadata().getGeneration())) {
                        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
                        connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                        LOGGER.info(
                            "Deployed resource drift detected observed_generation: {}, resource_generation: {}, move to phase: {}",
                            dr.getGeneration(),
                            res.getMetadata().getGeneration(),
                            connector.getStatus().getPhase());

                        return UpdateControl.updateStatusSubResource(connector);
                    }
                } else if (dr.getResourceVersion() != null) {
                    if (!Objects.equals(dr.getResourceVersion(), res.getMetadata().getResourceVersion())) {
                        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
                        connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                        LOGGER.info(
                            "Deployed resource drift detected observed_resource_version: {}, resource_version: {}, move to phase: {}",
                            dr.getResourceVersion(),
                            res.getMetadata().getResourceVersion(),
                            connector.getStatus().getPhase());

                        return UpdateControl.updateStatusSubResource(connector);
                    }
                }
            }
        }

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleError(ManagedConnector connector) {
        boolean updated = !Objects.equals(
            connector.getSpec().getDeployment(),
            connector.getStatus().getDeployment());

        if (updated) {
            JsonNode specNode = Serialization.jsonMapper().valueToTree(connector.getSpec().getDeployment());
            JsonNode statusNode = Serialization.jsonMapper().valueToTree(connector.getStatus().getDeployment());

            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
            connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Drift detected {}, move to phase: {}",
                JsonDiff.asJson(statusNode, specNode),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        // TODO: retry with backoff
        getRetryTimer().scheduleOnce(connector, 1500);

        return UpdateControl.noUpdate();
    }
}
