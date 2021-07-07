package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.zjsonpatch.JsonDiff;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentReifyRequest;
import org.bf2.cos.fleet.manager.api.model.meta.KafkaSpec;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.client.MetaClient;
import org.bf2.cos.fleetshard.operator.client.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEvent;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEventSource;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.ResourceUtil;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_GENERATED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_OPERATOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_TYPE_ID;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_ID;

@Controller(
    name = "connector",
    finalizerName = Controller.NO_FINALIZER)
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    private final Set<String> events = new HashSet<>();

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    MetaClient meta;
    @Inject
    ConnectorDeploymentStatusSync statusSync;

    @Override
    public void registerEventSources(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_operators",
            new ConnectorOperatorEventSource(kubernetesClient, fleetShard.getClusterNamespace()) {
                @Override
                protected void resourceUpdated(ManagedConnectorOperator resource) {
                    for (var connector : fleetShard.lookupManagedConnectors()) {
                        if (connector.getStatus() == null) {
                            continue;
                        }
                        if (connector.getStatus().getAssignedOperator() == null) {
                            continue;
                        }
                        if (!Objects.equals(resource.getSpec().getType(),
                            connector.getStatus().getAssignedOperator().getType())) {
                            continue;
                        }

                        final var rv = new Version(resource.getSpec().getVersion());
                        final var cv = new Version(connector.getStatus().getAssignedOperator().getVersion());

                        if (rv.compareTo(cv) > 0) {
                            getLogger().info("ManagedConnectorOperator updated, connector: {}/{}, operator: {}",
                                connector.getMetadata().getNamespace(),
                                connector.getMetadata().getName(),
                                resource.getSpec());

                            getEventHandler().handleEvent(
                                new ConnectorOperatorEvent(connector.getMetadata().getUid(), this));
                        }
                    }
                }
            });
    }

    @Override
    public UpdateControl<ManagedConnector> createOrUpdateResource(
        ManagedConnector connector,
        Context<ManagedConnector> context) {

        LOGGER.info("Reconcile {}/{}/{} (phase={})",
            connector.getApiVersion(),
            connector.getKind(),
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        if (connector.getStatus().getPhase() == null) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);
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
                break;
            }
            case DESIRED_STATE_READY: {
                final OperatorSelector selector = connector.getSpec().getOperatorSelector();
                final List<Operator> operators = fleetShard.lookupOperators();
                final Operator assigned = selector.assign(operators).orElseThrow(
                    () -> new IllegalStateException(
                        "Unable to find an operator for deployment: " + connector.getSpec().getDeployment()));

                connector.getStatus().setAssignedOperator(assigned);
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Augmentation);
                break;
            }
            default:
                throw new IllegalStateException(
                    "Unknown desired state: " + connector.getSpec().getDeployment().getDesiredState());
        }

        return UpdateControl.updateStatusSubResource(connector);
    }

    @SuppressWarnings("unchecked")
    private UpdateControl<ManagedConnector> handleAugmentation(ManagedConnector connector) {
        final DeploymentSpec ref = connector.getSpec().getDeployment();
        final String connectorId = connector.getMetadata().getName();

        try {
            ConnectorDeployment deployment = controlPlane.getDeployment(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId());

            KafkaSpec ks = new KafkaSpec()
                .id(deployment.getSpec().getKafkaId())
                .clientId(deployment.getSpec().getKafka().getClientId())
                .clientSecret(deployment.getSpec().getKafka().getClientSecret())
                .bootstrapServer(deployment.getSpec().getKafka().getBootstrapServer());

            ConnectorDeploymentReifyRequest rr = new ConnectorDeploymentReifyRequest()
                .connectorResourceVersion(deployment.getSpec().getConnectorResourceVersion())
                .deploymentResourceVersion(ref.getDeploymentResourceVersion())
                .managedConnectorId(connectorId)
                .deploymentId(connector.getSpec().getDeploymentId())
                .connectorId(deployment.getSpec().getConnectorId())
                .connectorTypeId(deployment.getSpec().getConnectorTypeId())
                .connectorSpec(deployment.getSpec().getConnectorSpec())
                .shardMetadata(deployment.getSpec().getShardMetadata())
                .kafkaSpec(ks);

            var answer = meta.reify(
                connector.getStatus().getAssignedOperator().getMetaService(),
                rr);

            if (answer.getResources() != null) {
                for (JsonNode node : answer.getResources()) {
                    ObjectNode on = (ObjectNode) node;
                    on.with("metadata")
                        .with("labels")
                        .put(LABEL_CONNECTOR_GENERATED, "true")
                        .put(LABEL_CONNECTOR_OPERATOR, connector.getStatus().getAssignedOperator().getId())
                        .put(LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                        .put(LABEL_CONNECTOR_TYPE_ID, deployment.getSpec().getConnectorTypeId())
                        .put(LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId());

                    on.with("metadata")
                        .withArray("ownerReferences")
                        .addObject()
                        .put("apiVersion", connector.getApiVersion())
                        .put("controller", true)
                        .put("kind", connector.getKind())
                        .put("name", connector.getMetadata().getName())
                        .put("uid", connector.getMetadata().getUid());

                    final var resource = uc.createOrReplace(connector.getMetadata().getNamespace(), node);
                    final var meta = (Map<String, Object>) resource.getOrDefault("metadata", Map.of());
                    final var annotations = (Map<String, String>) meta.get("annotations");
                    final var rApiVersion = (String) resource.get("apiVersion");
                    final var rKind = (String) resource.get("kind");
                    final var rName = (String) meta.get("name");
                    final var res = new DeployedResource(rApiVersion, rKind, rName);

                    if (DELETION_MODE_CONNECTOR.equals(annotations.get(ANNOTATION_DELETION_MODE))) {
                        if (!connector.getStatus().getResources().contains(res)) {
                            connector.getStatus().getResources().add(res);
                        }
                    } else {
                        res.setDeploymentRevision(ref.getDeploymentResourceVersion());
                        connector.getStatus().getResources().add(res);
                    }
                }
            }

            connector.getStatus().setDeployment(connector.getSpec().getDeployment());
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);

            return UpdateControl.updateStatusSubResource(connector);
        } catch (ConnectException e) {
            LOGGER.warn("Error connecting to meta service "
                + connector.getStatus().getAssignedOperator().getMetaService()
                + ", retrying");

            // TODO: remove once the SDK support re-scheduling
            //       https://github.com/java-operator-sdk/java-operator-sdk/issues/369
            // TODO: add back-off
            // TODO: better exception checking
            getRetryTimer().scheduleOnce(connector, 1500);
        } catch (Exception e) {
            LOGGER.warn("Error retrieving data from the meta service", e);
        }

        return UpdateControl.noUpdate();
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

            LOGGER.info("Drift detected {}, move to phase: {}",
                JsonDiff.asJson(statusNode, specNode),
                connector.getStatus().getPhase());
        }

        //
        // Set up watcher for resource types owned by the connectors. We don't
        // create a watch for each resource the connector owns to avoid creating
        // loads of watchers, instead we create a resource per type which will
        // triggers connectors based on the UUID of the owner (see the 'monitor'
        // method for more info)
        //
        for (var res : connector.getStatus().getResources()) {
            watchResource(connector, res);
        }

        //
        // Search for newly installed ManagedOperators
        //
        final List<Operator> operators = fleetShard.lookupOperators();
        final Operator assigned = connector.getStatus().getAssignedOperator();
        final Operator available = connector.getStatus().getAvailableOperator();
        final OperatorSelector selector = connector.getSpec().getOperatorSelector();

        var maybeAvailable = selector.available(operators)
            .filter(operator -> !Objects.equals(operator, assigned) && !Objects.equals(operator, available));

        if (maybeAvailable.isPresent()) {
            LOGGER.info("deployment (upd): {} -> from:{}, to: {}",
                connector.getSpec().getDeployment(),
                assigned,
                maybeAvailable.get());

            connector.getStatus().setAvailableOperator(maybeAvailable.get());
            updated = true;
        }

        try {
            updated |= !cleanupResources(connector).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!updated) {
            // no update to the resource but maybe a dependant resource has changed so we need
            // to trigger an update
            statusSync.submit(connector);
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

                LOGGER.info("Connector {} deleted, move to phase: {}",
                    connector.getMetadata().getName(),
                    connector.getStatus().getPhase());

                return UpdateControl.updateStatusSubResource(connector);
            }
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // TODO: reschedule a cleanup with backoff
        getRetryTimer().scheduleOnce(connector, 1500);

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleDeleted(ManagedConnector connector) {
        statusSync.submit(connector);

        // TODO: cleanup leftover, maybe
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
        if (connector.getStatus().getDeployment().getResourceVersion() == null) {
            return Collections.emptyList();
        }

        final var cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
        final var removed = new ArrayList<DeployedResource>();

        final var it = connector.getStatus().getResources().listIterator();
        while (it.hasNext()) {
            final var ref = it.next();

            if (!connector.getSpec().getDeployment().hasDesiredStateOf(DESIRED_STATE_DELETED)) {
                if (ref.getDeploymentRevision() == null) {
                    continue;
                }
                if (Objects.equals(cdrv, ref.getDeploymentRevision())) {
                    continue;
                }
            }

            if (uc.delete(connector.getMetadata().getNamespace(), ref)) {
                it.remove();
                removed.add(ref);
            }
        }

        for (var ref : removed) {
            LOGGER.info("Resource removed {}/{}/{} (deployment={})",
                ref.getApiVersion(),
                ref.getKind(),
                ref.getName(),
                connector.getSpec().getDeploymentId());
        }

        return removed;
    }

    /**
     * As we do not know what resources we have to deal with so we can create a watcher per
     * for each kind + apiVersion combination.
     *
     * @param connector the connector that holds the resources
     * @param resource  the resource to watch
     */
    private synchronized void watchResource(
        ManagedConnector connector,
        ResourceRef resource) {

        final String key = resource.getApiVersion() + ":" + resource.getKind();

        if (this.events.add(key)) {
            LOGGER.info("Registering an event for: {}", key);

            getEventSourceManager().registerEventSource(key, new WatcherEventSource<String>(kubernetesClient) {
                @SuppressWarnings("unchecked")
                @Override
                public void onEventReceived(Action action, String resource) {
                    try {
                        final Map<String, Object> unstructured = Serialization.jsonMapper().readValue(resource, Map.class);
                        final ResourceRef ref = ResourceUtil.asResourceRef(unstructured);
                        final ObjectMeta meta = ResourceUtil.getObjectMeta(unstructured);

                        LOGGER.info("Event received on resource: {}", ref);

                        //
                        // Since we need to know the owner UUID of the resource to properly
                        // generate the event, we can use the list of the owners
                        //
                        for (OwnerReference or : meta.getOwnerReferences()) {
                            getEventHandler().handleEvent(
                                new ResourceEvent(action, ref, or.getUid(), this));
                        }
                    } catch (JsonProcessingException e) {
                        throw KubernetesClientException.launderThrowable(e);
                    }
                }

                @Override
                protected Watch doWatch() {
                    return uc.watch(
                        connector.getMetadata().getNamespace(),
                        new ResourceRef(
                            resource.getApiVersion(),
                            resource.getKind(),
                            null),
                        Map.of(LABEL_CONNECTOR_GENERATED, "true"),
                        this);
                }
            });
        }
    }
}
