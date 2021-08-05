package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.zjsonpatch.JsonDiff;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.operand.OperandController;
import org.bf2.cos.fleetshard.operator.operand.OperandResourceWatcher;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.bf2.cos.fleetshard.support.Version;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.CONTEXT_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_OPERATOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_TYPE_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONTEXT;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_WATCH;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_FAILED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;
import static org.bf2.cos.fleetshard.support.OperatorSelectorUtil.assign;
import static org.bf2.cos.fleetshard.support.OperatorSelectorUtil.available;
import static org.bf2.cos.fleetshard.support.resources.ResourceUtil.getDeletionMode;
import static org.bf2.cos.fleetshard.support.resources.ResourceUtil.getDeploymentResourceVersion;

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
                new ConnectorOperatorEventSource());
            eventSourceManager.registerEventSource(
                "_secrets",
                new OperandResourceWatcher(kubernetesClient, "v1", "Secret"));

            for (CustomResourceDefinitionContext res : operandController.getResourceTypes()) {
                if ("v1".equals(res.getVersion()) && "Secret".equals(res.getKind())) {
                    continue;
                }

                eventSourceManager.registerEventSource(
                    "_" + res.getGroup() + "/" + res.getVersion() + ":" + res.getKind(),
                    new OperandResourceWatcher(kubernetesClient, res));
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
                final OperatorSelector selector = connector.getSpec().getOperatorSelector();
                final List<Operator> operators = fleetShard.lookupOperators();
                final Operator assigned = assign(selector, operators).orElseThrow(
                    () -> new IllegalStateException(
                        "Unable to find an operator for deployment: " + connector.getSpec().getDeployment()));

                connector.getStatus().setAssignedOperator(assigned);
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
        final String connectorId = connector.getMetadata().getName();

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
            final var condition = new ConditionBuilder()
                .withType(ManagedConnectorStatus.ConditionType.Ready.name())
                .withStatus(ManagedConnectorStatus.ConditionStatus.False.name())
                .withReason("SecretNotFound")
                .withMessage("Unable to find secret with name: " + connector.getSpec().getDeployment().getSecret())
                .withLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                .build();

            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Error);
            connector.getStatus().setConditions(List.of(condition));
            connector.getStatus().getConnectorStatus().setPhase(STATE_FAILED);
            connector.getStatus().setConditions(List.of(condition));

            return UpdateControl.updateStatusSubResource(connector);
        }

        for (var resource : operandController.reify(connector.getSpec(), secret)) {
            if (resource.getMetadata().getLabels() == null) {
                resource.getMetadata().setLabels(new HashMap<>());
            }
            if (resource.getMetadata().getAnnotations() == null) {
                resource.getMetadata().setAnnotations(new HashMap<>());
            }

            final String rv = Long.toString(connector.getSpec().getDeployment().getDeploymentResourceVersion());
            final String deletionMode = getDeletionMode(resource).orElse(DELETION_MODE_CONNECTOR);
            final DeployedResource res = DeployedResource.of(resource);

            final Map<String, String> labels = KubernetesResourceUtil.getOrCreateLabels(resource);
            labels.put(LABEL_WATCH, "true");
            labels.put(LABEL_CONTEXT, CONTEXT_CONNECTOR);
            labels.put(LABEL_CONNECTOR_OPERATOR, connector.getStatus().getAssignedOperator().getId());
            labels.put(LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId());
            labels.put(LABEL_CONNECTOR_TYPE_ID, connector.getSpec().getDeployment().getConnectorTypeId());
            labels.put(LABEL_DEPLOYMENT_RESOURCE_VERSION, rv);
            labels.put(LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId());
            labels.put(LABEL_CLUSTER_ID, connector.getSpec().getClusterId());

            resource.getMetadata().setOwnerReferences(List.of(
                new OwnerReferenceBuilder()
                    .withApiVersion(connector.getApiVersion())
                    .withKind(connector.getKind())
                    .withName(connector.getMetadata().getName())
                    .withUid(connector.getMetadata().getUid())
                    .withBlockOwnerDeletion(true)
                    .build()));

            LOGGER.debug("createOrReplace: {}", res);
            uc.createOrReplace(connector.getMetadata().getNamespace(), resource);

            if (!connector.getStatus().getResources().contains(res)) {
                connector.getStatus().getResources().add(res);
            }

            if (!DELETION_MODE_CONNECTOR.equals(deletionMode)) {
                res.setDeploymentRevision(ref.getDeploymentResourceVersion());
            }
        }

        // Add the secret created by the sync among the list of resources to
        // clean-up upon delete/update
        DeployedResource res = DeployedResource.of(secret);
        res.setDeploymentRevision(getDeploymentResourceVersion(secret));

        if (!connector.getStatus().getResources().contains(res)) {
            connector.getStatus().getResources().add(res);
        }

        connector.getStatus().setDeployment(connector.getSpec().getDeployment());
        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);
        connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

        return UpdateControl.updateStatusSubResource(connector);
    }

    // TODO: check for changes to the underlying resources
    // TODO: check for checksum mismatch
    private UpdateControl<ManagedConnector> handleMonitor(ManagedConnector connector) {
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
        } else {
            if (connector.getStatus().getAssignedOperator() != null && connector.getStatus().getResources() != null) {
                operandController.status(connector.getStatus());
                updated = true;
            }
        }

        //
        // Search for newly installed ManagedOperators
        //
        final List<Operator> operators = fleetShard.lookupOperators();
        final Operator assignedOperator = connector.getStatus().getAssignedOperator();
        final Operator availableOperator = connector.getStatus().getAvailableOperator();
        final OperatorSelector selector = connector.getSpec().getOperatorSelector();

        var maybeAvailable = available(selector, operators)
            .filter(operator -> !Objects.equals(operator, assignedOperator) && !Objects.equals(operator, availableOperator));

        if (maybeAvailable.isPresent()) {
            LOGGER.info("deployment (upd): {} -> from:{}, to: {}",
                connector.getSpec().getDeployment(),
                assignedOperator,
                maybeAvailable.get());

            connector.getStatus().setAvailableOperator(maybeAvailable.get());
            updated = true;
        }

        try {
            updated |= !cleanupResources(connector).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return updated
            ? UpdateControl.updateStatusSubResource(connector)
            : UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleDeleting(ManagedConnector connector) {
        LOGGER.info("Deleting connector: {}", connector.getMetadata().getName());

        try {
            cleanupResources(connector);

            if (connector.getStatus().getResources().isEmpty()) {
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Deleted);
                connector.getStatus().getConnectorStatus().setPhase(STATE_DELETED);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                LOGGER.info("Connector {} deleted, move to phase: {}",
                    connector.getMetadata().getName(),
                    connector.getStatus().getPhase());

                return UpdateControl.updateStatusSubResource(connector);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO: reschedule a cleanup with backoff
        getRetryTimer().scheduleOnce(connector, 1500);

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleDeleted(ManagedConnector connector) {
        // TODO: cleanup leftover, maybe
        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleStopping(ManagedConnector connector) {
        LOGGER.info("Stopping connector: {}", connector.getMetadata().getName());

        try {
            cleanupResources(connector);

            if (connector.getStatus().getResources().isEmpty()) {
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Stopped);
                connector.getStatus().getConnectorStatus().setPhase(STATE_STOPPED);
                connector.getStatus().getConnectorStatus().setConditions(Collections.emptyList());

                LOGGER.info("Connector {} stopped, move to phase: {}",
                    connector.getMetadata().getName(),
                    connector.getStatus().getPhase());

                return UpdateControl.updateStatusSubResource(connector);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        // TODO: cleanup leftover, maybe
        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleError(ManagedConnector connector) {
        // TODO: handle failure, maybe retry ?
        return UpdateControl.noUpdate();
    }

    // **************************************************
    //
    // Helpers
    //
    // **************************************************

    /**
     * It may happen that the resources associated with a connector change between deployments,
     * as example for camel-k the secrets will be named after the resource version so we need
     * to remove potential orphaned resources.
     *
     * @param connector the connector.
     */
    private List<DeployedResource> cleanupResources(ManagedConnector connector) throws IOException {
        if (connector.getStatus().getDeployment().getDeploymentResourceVersion() == null) {
            return Collections.emptyList();
        }

        final var cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
        final var removed = new ArrayList<DeployedResource>();

        for (var it = connector.getStatus().getResources().iterator(); it.hasNext();) {
            final var ref = it.next();
            final var deployment = connector.getSpec().getDeployment();

            if (!deployment.hasDesiredStateOf(DESIRED_STATE_DELETED, DESIRED_STATE_STOPPED)) {
                if (ref.getDeploymentRevision() == null) {
                    continue;
                }
                if (Objects.equals(cdrv, ref.getDeploymentRevision())) {
                    continue;
                }
            }

            uc.delete(connector.getMetadata().getNamespace(), ref);

            if (uc.get(connector.getMetadata().getNamespace(), ref) == null) {
                it.remove();
                removed.add(ref);
            }
        }

        for (var ref : removed) {
            LOGGER.info("gc: resource removed {}:{}:{} (deployment={})",
                ref.getApiVersion(),
                ref.getKind(),
                ref.getName(),
                connector.getSpec().getDeploymentId());
        }

        return removed;
    }

    private class ConnectorOperatorEventSource extends WatcherEventSource<ManagedConnectorOperator> {
        public ConnectorOperatorEventSource() {
            super(kubernetesClient);
        }

        @Override
        protected Watch doWatch() {
            return getClient()
                .customResources(ManagedConnectorOperator.class)
                .inNamespace(fleetShard.getOperatorNamespace())
                .watch(this);
        }

        @Override
        protected void onEventReceived(Action action, ManagedConnectorOperator resource) {
            LOGGER.debug("Event {} received for managed connector operator: {}/{}",
                action.name(),
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getName());

            getEventHandler().handleEvent(
                new DefaultEvent(
                    cr -> hasGreaterVersion((ManagedConnector) cr, resource),
                    this));
        }

        private boolean hasGreaterVersion(ManagedConnector connector, ManagedConnectorOperator resource) {
            if (connector.getStatus() == null) {
                return false;
            }
            if (connector.getStatus().getAssignedOperator() == null) {
                return false;
            }
            if (!Objects.equals(resource.getSpec().getType(),
                connector.getStatus().getAssignedOperator().getType())) {
                return false;
            }

            final var rv = new Version(resource.getSpec().getVersion());
            final var cv = new Version(connector.getStatus().getAssignedOperator().getVersion());

            return rv.compareTo(cv) > 0;
        }
    }
}
