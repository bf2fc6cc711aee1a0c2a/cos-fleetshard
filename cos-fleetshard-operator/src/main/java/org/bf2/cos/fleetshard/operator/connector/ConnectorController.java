package org.bf2.cos.fleetshard.operator.connector;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.DeploymentSpecAware;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorConditions;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorBuilder;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.operand.OperandController;
import org.bf2.cos.fleetshard.operator.operand.OperandControllerMetricsWrapper;
import org.bf2.cos.fleetshard.operator.operand.OperandResourceWatcher;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnectorConditions.hasCondition;
import static org.bf2.cos.fleetshard.api.ManagedConnectorConditions.setCondition;
import static org.bf2.cos.fleetshard.support.OperatorSelectorUtil.available;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_OPERATOR;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_TYPE_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_OWNER;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_OPERATOR_TYPE;
import static org.bf2.cos.fleetshard.support.resources.Resources.copyAnnotation;
import static org.bf2.cos.fleetshard.support.resources.Resources.copyLabel;

@Controller(
    name = "connector",
    finalizerName = Controller.NO_FINALIZER,
    generationAwareEventProcessing = false)
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    @Inject
    ManagedConnectorOperator managedConnectorOperator;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    OperandController wrappedOperandController;

    @Inject
    MeterRegistry registry;
    @Inject
    FleetShardOperatorConfig config;

    private OperandController operandController;
    private List<Tag> tags;

    @PostConstruct
    protected void setUp() {
        this.tags = List.of(
            Tag.of("cos.operator.id", managedConnectorOperator.getMetadata().getName()),
            Tag.of("cos.operator.type", managedConnectorOperator.getSpec().getType()),
            Tag.of("cos.operator.version", managedConnectorOperator.getSpec().getVersion()));

        if (config.metrics().connectorOperand().enabled()) {
            operandController = new OperandControllerMetricsWrapper(
                wrappedOperandController,
                MetricsRecorder.of(registry, config.metrics().baseName() + ".controller.event.operators.operand", tags));
        } else {
            operandController = wrappedOperandController;
        }
    }

    @Override
    public void registerEventSources(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_secrets",
            new ConnectorSecretEventSource(
                kubernetesClient,
                managedConnectorOperator,
                fleetShard.getOperatorNamespace(),
                MetricsRecorder.of(registry, config.metrics().baseName() + ".controller.event.secrets", tags)));
        eventSourceManager.registerEventSource(
            "_operators",
            new ConnectorOperatorEventSource(
                kubernetesClient,
                managedConnectorOperator,
                fleetShard.getOperatorNamespace(),
                MetricsRecorder.of(registry, config.metrics().baseName() + ".controller.event.operators", tags)));

        for (ResourceDefinitionContext res : operandController.getResourceTypes()) {
            final String id = res.getGroup() + "-" + res.getVersion() + "-" + res.getKind();

            eventSourceManager.registerEventSource(
                id,
                new OperandResourceWatcher(
                    kubernetesClient,
                    managedConnectorOperator,
                    res,
                    fleetShard.getConnectorsNamespace(),
                    MetricsRecorder.of(registry, id, tags)));
        }
    }

    @Override
    protected UpdateControl<ManagedConnector> reconcile(
        ManagedConnector connector,
        Context<ManagedConnector> context) {

        LOGGER.info("Reconcile {}:{}:{}@{} (phase={})",
            connector.getApiVersion(),
            connector.getKind(),
            connector.getMetadata().getName(),
            connector.getMetadata().getNamespace(),
            connector.getStatus().getPhase());

        final boolean selected = selected(connector);
        final boolean assigned = assigned(connector);

        final UpdateControl<ManagedConnector> answer;

        if (!selected && !assigned) {
            // not selected, nor assigned: this connector is managed by another operator
            LOGGER.debug("Connector {}/{} is not managed by this operator (assigned={}, operating={}).",
                connector.getMetadata().getNamespace(),
                connector.getMetadata().getName(),
                connector.getSpec().getOperatorSelector().getId(),
                connector.getStatus().getConnectorStatus().getAssignedOperator().getId());

            answer = UpdateControl.noUpdate();
        } else if (!selected) {
            // not selected, but assigned: this connector needs to be handed to another operator
            LOGGER.debug("Connector {}/{} not selected but assigned: this connector needs to be handed to {} operator.",
                connector.getMetadata().getNamespace(),
                connector.getMetadata().getName(),
                connector.getSpec().getOperatorSelector().getId());

            if (!ManagedConnectorStatus.PhaseType.Error.equals(connector.getStatus().getPhase())
                && !ManagedConnectorStatus.PhaseType.Transferring.equals(connector.getStatus().getPhase())
                && !ManagedConnectorStatus.PhaseType.Transferred.equals(connector.getStatus().getPhase())) {
                // the connector needs to be transferred to another operator
                LOGGER.debug("Connector {}/{} needs to be transferred to {} operator.",
                    connector.getMetadata().getNamespace(),
                    connector.getMetadata().getName(),
                    connector.getSpec().getOperatorSelector().getId());

                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Transferring);
                answer = UpdateControl.updateStatusSubResource(connector);
            } else {
                // the connector is transferring to another operator, just reconcile and wait.
                LOGGER.debug("Connector {}/{}, is transferring to {} operator, reconcile and wait.",
                    connector.getMetadata().getNamespace(),
                    connector.getMetadata().getName(),
                    connector.getSpec().getOperatorSelector().getId());

                answer = reconcile(connector);
            }
        } else if (!assigned) {
            // not assigned, but selected: this connector is being transferred to this operator.
            LOGGER.debug("Connector {}/{} not assigned, but selected.",
                connector.getMetadata().getNamespace(),
                connector.getMetadata().getName());

            if (connector.getStatus().getConnectorStatus().getAssignedOperator().getId() == null) {
                // this operator can start to manage this connector.
                LOGGER.debug("Connector {}/{} has just being assigned to this operator ({}), starting to manage it.",
                    connector.getMetadata().getNamespace(),
                    connector.getMetadata().getName(),
                    connector.getSpec().getOperatorSelector().getId());
                answer = reconcile(connector);
            } else {
                // transferring to this operator is still in progress, let's wait.
                LOGGER.debug("Skip connector: waiting for connector {}/{} to be transferred from operator: {}.",
                    connector.getMetadata().getNamespace(),
                    connector.getMetadata().getName(),
                    connector.getStatus().getConnectorStatus().getAssignedOperator().getId());

                answer = UpdateControl.noUpdate();
            }
        } else {
            // connector is assigned to this operator, reconcile it.
            LOGGER.debug("Connector: {}/{} is managed by this operator (assigned={}, operating={}).",
                connector.getMetadata().getNamespace(),
                connector.getMetadata().getName(),
                connector.getSpec().getOperatorSelector().getId(),
                connector.getStatus().getConnectorStatus().getAssignedOperator().getId());

            answer = reconcile(connector);
        }

        return answer;
    }

    private UpdateControl<ManagedConnector> reconcile(ManagedConnector resource) {
        if (resource.getStatus().getPhase() == null) {
            resource.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
            resource.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
            resource.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
        }

        return measure(
            config.metrics().baseName()
                + ".controller.connectors.reconcile."
                + resource.getStatus().getPhase().name().toLowerCase(Locale.US),
            resource,
            connector -> {
                switch (connector.getStatus().getPhase()) {
                    case Initialization:
                        return handleInitialization(connector);
                    case Augmentation:
                        return handleAugmentation(connector);
                    case Monitor:
                        return validate(connector, this::handleMonitor);
                    case Deleting:
                        return handleDeleting(connector);
                    case Deleted:
                        return handleDeleted(connector);
                    case Stopping:
                        return handleStopping(connector);
                    case Stopped:
                        return validate(connector, this::handleStopped);
                    case Transferring:
                        return handleTransferring(connector);
                    case Transferred:
                        return handleTransferred(connector);
                    case Error:
                        return validate(connector, this::handleError);
                    default:
                        throw new UnsupportedOperationException(
                            "Unsupported phase: " + connector.getStatus().getPhase());
                }
            });
    }

    private UpdateControl<ManagedConnector> measure(
        String id,
        ManagedConnector connector,
        Function<ManagedConnector, UpdateControl<ManagedConnector>> action) {

        Counter.builder(id + ".count")
            .tags(tags)
            .tag("cos.connector.id", connector.getSpec().getConnectorId())
            .tag("cos.deployment.id", connector.getSpec().getDeploymentId())
            .tag("cos.deployment.resync", Boolean.toString(isResync(connector)))
            .register(registry)
            .increment();

        try {
            return Timer.builder(id + ".time")
                .tags(tags)
                .tag("cos.connector.id", connector.getSpec().getConnectorId())
                .tag("cos.deployment.id", connector.getSpec().getDeploymentId())
                .tag("cos.deployment.resync", Boolean.toString(isResync(connector)))
                .publishPercentiles(0.3, 0.5, 0.95)
                .publishPercentileHistogram()
                .register(registry)
                .recordCallable(() -> action.apply(connector));
        } catch (Exception e) {
            throw new RuntimeException("Failure recording method execution (id: " + id + ")", e);
        }
    };

    // **************************************************
    //
    // Handlers
    //
    // **************************************************

    private UpdateControl<ManagedConnector> handleInitialization(ManagedConnector connector) {
        ManagedConnectorConditions.clearConditions(connector);

        setCondition(connector, ManagedConnectorConditions.Type.Initialization, true);
        setCondition(connector, ManagedConnectorConditions.Type.Ready, false, "Initialization");

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

                setCondition(connector, ManagedConnectorConditions.Type.Augmentation, true);
                setCondition(connector, ManagedConnectorConditions.Type.Ready, false);

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
        if (connector.getSpec().getDeployment().getSecret() == null) {
            LOGGER.info("Secret for deployment not defines");
            return UpdateControl.noUpdate();
        }

        Secret secret = kubernetesClient.secrets()
            .inNamespace(fleetShard.getConnectorsNamespace())
            .withName(connector.getSpec().getDeployment().getSecret())
            .get();

        if (secret == null) {
            boolean retry = hasCondition(
                connector,
                ManagedConnectorConditions.Type.Augmentation,
                ManagedConnectorConditions.Status.False,
                "SecretNotFound");

            if (!retry) {
                LOGGER.debug(
                    "Unable to find secret with name: {}", connector.getSpec().getDeployment().getSecret());

                setCondition(
                    connector,
                    ManagedConnectorConditions.Type.Augmentation,
                    ManagedConnectorConditions.Status.False,
                    "SecretNotFound",
                    "Unable to find secret with name: " + connector.getSpec().getDeployment().getSecret());
                setCondition(
                    connector,
                    ManagedConnectorConditions.Type.Ready,
                    ManagedConnectorConditions.Status.False,
                    "AugmentationError",
                    "AugmentationError");

                return UpdateControl.updateStatusSubResource(connector);
            } else {
                return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
            }
        } else {
            final String connectorUow = connector.getSpec().getDeployment().getUnitOfWork();
            final String secretUow = secret.getMetadata().getLabels().get(Resources.LABEL_UOW);

            if (!Objects.equals(connectorUow, secretUow)) {
                boolean retry = hasCondition(
                    connector,
                    ManagedConnectorConditions.Type.Augmentation,
                    ManagedConnectorConditions.Status.False,
                    "SecretUoWMismatch");

                if (!retry) {
                    LOGGER.debug(
                        "Secret and Connector UoW mismatch (connector: {}, secret: {})", connectorUow, secretUow);

                    setCondition(
                        connector,
                        ManagedConnectorConditions.Type.Augmentation,
                        ManagedConnectorConditions.Status.False,
                        "SecretUoWMismatch",
                        "Secret and Connector UoW mismatch (connector: " + connectorUow + ", secret: " + secretUow + ")");
                    setCondition(
                        connector,
                        ManagedConnectorConditions.Type.Ready,
                        ManagedConnectorConditions.Status.False,
                        "AugmentationError",
                        "AugmentationError");

                    return UpdateControl.updateStatusSubResource(connector);
                } else {
                    return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
                }
            }
        }

        for (var resource : operandController.reify(connector, secret)) {
            if (resource.getMetadata().getLabels() == null) {
                resource.getMetadata().setLabels(new HashMap<>());
            }
            if (resource.getMetadata().getAnnotations() == null) {
                resource.getMetadata().setAnnotations(new HashMap<>());
            }

            final String rv = Long.toString(connector.getSpec().getDeployment().getDeploymentResourceVersion());

            final Map<String, String> labels = KubernetesResourceUtil.getOrCreateLabels(resource);
            labels.put(LABEL_CONNECTOR_OPERATOR, connector.getStatus().getConnectorStatus().getAssignedOperator().getId());
            labels.put(LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId());
            labels.put(LABEL_CONNECTOR_TYPE_ID, connector.getSpec().getDeployment().getConnectorTypeId());
            labels.put(LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId());
            labels.put(LABEL_CLUSTER_ID, connector.getSpec().getClusterId());
            labels.put(LABEL_OPERATOR_TYPE, managedConnectorOperator.getSpec().getType());
            labels.put(LABEL_OPERATOR_OWNER, managedConnectorOperator.getMetadata().getName());
            labels.put(LABEL_DEPLOYMENT_RESOURCE_VERSION, rv);

            config.connectors().targetLabels().ifPresent(items -> {
                for (String item : items) {
                    copyLabel(item, connector, resource);
                }
            });
            config.connectors().targetAnnotations().ifPresent(items -> {
                for (String item : items) {
                    copyAnnotation(item, connector, resource);
                }
            });

            resource.getMetadata().setOwnerReferences(List.of(
                new OwnerReferenceBuilder()
                    .withApiVersion(connector.getApiVersion())
                    .withKind(connector.getKind())
                    .withName(connector.getMetadata().getName())
                    .withUid(connector.getMetadata().getUid())
                    .withBlockOwnerDeletion(true)
                    .build()));

            var result = kubernetesClient.resource(resource)
                .inNamespace(connector.getMetadata().getNamespace())
                .createOrReplace();

            LOGGER.debug("Resource {}:{}:{}@{} updated/created",
                result.getApiVersion(),
                result.getKind(),
                result.getMetadata().getName(),
                result.getMetadata().getNamespace());
        }

        connector.getStatus().setDeployment(connector.getSpec().getDeployment());
        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);
        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

        setCondition(connector, ManagedConnectorConditions.Type.Resync, false);
        setCondition(connector, ManagedConnectorConditions.Type.Monitor, true);
        setCondition(connector, ManagedConnectorConditions.Type.Ready, true);
        setCondition(connector, ManagedConnectorConditions.Type.Augmentation, true);

        return UpdateControl.updateStatusSubResource(connector);
    }

    private UpdateControl<ManagedConnector> handleMonitor(ManagedConnector connector) {
        operandController.status(connector);

        //
        // Search for newly installed ManagedOperators
        //
        final List<Operator> operators = fleetShard.lookupOperators();
        final Operator assignedOperator = connector.getStatus().getConnectorStatus().getAssignedOperator();
        final Operator availableOperator = connector.getStatus().getConnectorStatus().getAvailableOperator();
        final Optional<Operator> selected = available(connector.getSpec().getOperatorSelector(), operators);

        if (selected.isPresent()) {
            Operator selectedInstance = selected.get();

            // if the selected operator does match the operator preciously selected
            if (!Objects.equals(selectedInstance, availableOperator) && !Objects.equals(selectedInstance, assignedOperator)) {
                // and it is not the currently assigned one
                LOGGER.info("deployment (upd): {} -> from:{}, to: {}",
                    connector.getSpec().getDeployment(),
                    assignedOperator,
                    selectedInstance);

                // then we can signal that an upgrade is possible
                connector.getStatus().getConnectorStatus().setAvailableOperator(selectedInstance);
            }
        } else {
            connector.getStatus().getConnectorStatus().setAvailableOperator(new Operator());
        }

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

        return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
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

        return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private UpdateControl<ManagedConnector> handleStopped(ManagedConnector ignored) {
        return UpdateControl.noUpdate();
    }

    @SuppressWarnings({ "PMD.UnusedFormalParameter" })
    private UpdateControl<ManagedConnector> handleError(ManagedConnector connector) {
        return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
    }

    private UpdateControl<ManagedConnector> handleTransferring(ManagedConnector connector) {
        if (operandController.stop(connector)) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Transferred);
            connector.getStatus().getConnectorStatus().setPhase(STATE_STOPPED);
            connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

            LOGGER.info("Connector {} transferred, move to phase: {}",
                connector.getMetadata().getName(),
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        return UpdateControl.<ManagedConnector> noUpdate().withReSchedule(1500, TimeUnit.MILLISECONDS);
    }

    private UpdateControl<ManagedConnector> handleTransferred(ManagedConnector connector) {
        LOGGER.info("Connector {} complete, it can now be handled by another operator.",
            connector.getMetadata().getName());

        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
        connector.getStatus().getConnectorStatus().setAssignedOperator(null);
        connector.getStatus().getConnectorStatus().setAvailableOperator(null);

        return UpdateControl.updateStatusSubResource(connector);
    }

    // **************************************************
    //
    // Helpers
    //
    // **************************************************

    private UpdateControl<ManagedConnector> validate(
        ManagedConnector connector,
        Function<ManagedConnector, UpdateControl<ManagedConnector>> okAction) {

        if (!Objects.equals(connector.getSpec().getDeployment(), connector.getStatus().getDeployment())) {
            JsonNode specNode = Serialization.jsonMapper().valueToTree(connector.getSpec().getDeployment());
            JsonNode statusNode = Serialization.jsonMapper().valueToTree(connector.getStatus().getDeployment());
            JsonNode diff = JsonDiff.asJson(statusNode, specNode);

            if (diff.isArray() && diff.size() == 1 && diff.get(0).at("/path").asText().equals("/unitOfWork")) {
                final Long specResourceVersion = getDeploymentResourceVersion(connector.getSpec());
                final Long statResourceVersion = getDeploymentResourceVersion(connector.getStatus());

                if (specResourceVersion != null && specResourceVersion.equals(statResourceVersion)) {
                    //
                    // In case of re-sink, the diff should looks like:
                    //
                    //   [{
                    //      "op": "replace",
                    //      "path": "/unitOfWork",
                    //      "value": "61ba142d2a83cb1d0cf3dff2"
                    //   }]
                    //
                    // if the only changed element is unitOfWork then this reconcile loop was triggered
                    // by a re-sink process: to be on the safe side, the Augmentation step is re-executed
                    // as if it were a new connector so operand's CRs are re-generated.
                    //

                    setCondition(connector, ManagedConnectorConditions.Type.Resync, true);
                }
            }

            if (isResync(connector)) {
                setCondition(connector, ManagedConnectorConditions.Type.Augmentation, true, "Resync");
                setCondition(connector, ManagedConnectorConditions.Type.Ready, false, "Resync");

                //
                // If the managed connector is performing a resync, then we don't change the status
                // of the connector on the control plane as this is a technical phase. We can then
                // jump straight to the Augmentation phase
                //
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Augmentation);
            } else {
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
                connector.getStatus().getConnectorStatus().setPhase(STATE_PROVISIONING);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());
            }

            LOGGER.info("Drift detected on connector deployment {}: {} -> move to phase: {}",
                connector.getSpec().getDeploymentId(),
                diff,
                connector.getStatus().getPhase());

            return UpdateControl.updateStatusSubResource(connector);
        }

        return okAction.apply(connector);
    }

    private boolean selected(ManagedConnector connector) {
        return connector.getSpec().getOperatorSelector() != null
            && Objects.equals(
                managedConnectorOperator.getMetadata().getName(),
                connector.getSpec().getOperatorSelector().getId());
    }

    private boolean assigned(ManagedConnector connector) {
        return connector.getStatus().getConnectorStatus().getAssignedOperator() != null
            && Objects.equals(
                managedConnectorOperator.getMetadata().getName(),
                connector.getStatus().getConnectorStatus().getAssignedOperator().getId());
    }

    private Long getDeploymentResourceVersion(DeploymentSpecAware spec) {
        if (spec == null) {
            return null;
        }
        if (spec.getDeployment() == null) {
            return null;
        }

        return spec.getDeployment().getDeploymentResourceVersion();
    }

    private boolean isResync(ManagedConnector connector) {
        return hasCondition(
            connector,
            ManagedConnectorConditions.Type.Resync,
            ManagedConnectorConditions.Status.True,
            "Resync");
    }

}
