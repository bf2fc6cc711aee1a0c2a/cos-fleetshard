package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
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
import io.fabric8.kubernetes.api.model.Condition;
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
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorOperator;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleet.manager.api.model.cp.MetaV1Condition;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentReifyRequest;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.api.model.meta.KafkaSpec;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEventSource;
import org.bf2.cos.fleetshard.operator.fleet.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.fleet.FleetShardClient;
import org.bf2.cos.fleetshard.operator.it.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.it.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.it.support.ResourceUtil;
import org.bf2.cos.fleetshard.operator.it.support.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.it.support.WatcherEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_GENERATED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_CONNECTOR_OPERATOR;

@Controller(
    name = "connector")
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    private final Set<String> events;
    private final TimerEventSource retryTimer;
    private final Vertx vertx;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    private EventSourceManager eventSourceManager;

    public ConnectorController(io.vertx.core.Vertx vertx) {
        this.events = new HashSet<>();
        this.retryTimer = new TimerEventSource();
        this.vertx = new Vertx(vertx);
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        this.eventSourceManager = eventSourceManager;
        this.eventSourceManager.registerEventSource(
            "_connector-retry-timer",
            retryTimer);
        this.eventSourceManager.registerEventSource(
            "_connector-operator",
            new ConnectorOperatorEventSource(kubernetesClient, fleetShard.getConnectorsNamespace()));
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
            return handleInitialization(connector);
        }

        switch (connector.getStatus().getPhase()) {
            case Initialization:
                return handleInitialization(connector);
            case Augmentation:
                return handleAugmentation(connector);
            case Monitor:
                return handleMonitor(connector);
            case Delete:
                return handleDelete(connector);
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

        controlPlane.updateConnectorStatus(
            connector,
            new ConnectorDeploymentStatus().phase("provisioning"));

        switch (connector.getSpec().getDeployment().getDesiredState()) {
            case DESIRED_STATE_DELETED: {
                connector.getStatus().setDeployment(connector.getSpec().getDeployment());
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Delete);
                break;
            }
            case DESIRED_STATE_READY: {
                final OperatorSelector selector = connector.getSpec().getDeployment().getOperatorSelector();
                final List<Operator> operators = fleetShard.lookupOperators();

                selector.assign(operators).ifPresentOrElse(
                    operator -> {
                        LOGGER.info("deployment (init): {} -> operator: {}",
                            connector.getSpec().getDeployment(),
                            operator);

                        connector.getStatus().setOperator(operator);
                    },
                    () -> {
                        throw new IllegalArgumentException(
                            "Unable to determine operator for deployment: " + connector.getStatus().getDeployment());
                    });

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
        final String connectorMeta = connector.getStatus().getOperator().getMetaService();

        try {
            ConnectorDeployment deployment = controlPlane.getDeployment(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId());

            ConnectorDeploymentSpec cdspec;
            WebClient client = WebClient.create(vertx);

            try {
                LOGGER.info("Connecting to meta service at : {}", connectorMeta);

                final String[] hp = connectorMeta.split(":");
                final String host = hp[0];
                final int port = hp.length == 2 ? Integer.parseInt(hp[1]) : 80;

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
                    .connectorId(deployment.getSpec().getConnectorTypeId())
                    .connectorSpec(deployment.getSpec().getConnectorSpec())
                    .shardMetadata(deployment.getSpec().getShardMetadata())
                    .kafkaSpec(ks);

                // TODO: set-up ssl/tls
                cdspec = client.post(port, host, "/reify")
                    .sendJson(rr)
                    .await()
                    .atMost(Duration.ofSeconds(30))
                    .bodyAsJson(ConnectorDeploymentSpec.class);
            } finally {
                client.close();
            }

            if (cdspec.getResources() != null) {
                for (JsonNode node : cdspec.getResources()) {
                    LOGGER.info("Got {}", Serialization.asJson(node));

                    ObjectNode on = (ObjectNode) node;
                    on.with("metadata")
                        .with("labels")
                        .put(LABEL_CONNECTOR_GENERATED, "true")
                        .put(LABEL_CONNECTOR_OPERATOR, connector.getStatus().getOperator().getId());
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

            connector.getStatus().setStatusExtractor(cdspec.getStatusExtractor());
            connector.getStatus().setDeployment(connector.getSpec().getDeployment());
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);

            return UpdateControl.updateStatusSubResource(connector);
        } catch (ConnectException e) {
            LOGGER.warn("Error connecting to meta service " + connectorMeta + ", retrying");
            // TODO: remove once the SDK support re-scheduling
            //       https://github.com/java-operator-sdk/java-operator-sdk/issues/369
            // TODO: add back-off
            // TODO: better exception checking
            retryTimer.scheduleOnce(connector, 1500);
        } catch (Exception e) {
            LOGGER.warn("Error retrieving data from the meta service", e);
        }

        return UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleMonitor(ManagedConnector connector) {

        boolean updated = !Objects.equals(
            connector.getSpec().getDeployment(),
            connector.getStatus().getDeployment());

        if (updated) {
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Initialization);

            JsonNode specNode = Serialization.jsonMapper().valueToTree(connector.getSpec().getDeployment());
            JsonNode statusNode = Serialization.jsonMapper().valueToTree(connector.getStatus().getDeployment());

            LOGGER.info("Drift detected {}, move to phase: {}",
                JsonDiff.asJson(statusNode, specNode),
                connector.getStatus().getPhase());
        }

        try {
            ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
            ds.setResourceVersion(connector.getStatus().getDeployment().getDeploymentResourceVersion());

            extractStatus(connector, ds);
            checkForAvailableUpdates(connector, ds);

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

            controlPlane.updateConnectorStatus(connector, ds);
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            var removed = cleanupResources(connector);
            if (!removed.isEmpty()) {
                for (var ref : removed) {
                    LOGGER.info("Resource removed {}/{}/{} (deployment={})",
                        ref.getApiVersion(),
                        ref.getKind(),
                        ref.getName(),
                        connector.getSpec().getDeploymentId());
                }

                updated = true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return updated
            ? UpdateControl.updateStatusSubResource(connector)
            : UpdateControl.noUpdate();
    }

    private UpdateControl<ManagedConnector> handleDelete(ManagedConnector connector) {
        boolean updated = false;

        try {
            var removed = cleanupResources(connector);
            if (!removed.isEmpty()) {
                for (var ref : removed) {
                    LOGGER.info("Resource removed {}/{}/{} (deployment={})",
                        ref.getApiVersion(),
                        ref.getKind(),
                        ref.getName(),
                        connector.getSpec().getDeploymentId());
                }

                updated = true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
            ds.setResourceVersion(connector.getSpec().getDeployment().getDeploymentResourceVersion());
            ds.setPhase("deleted");

            controlPlane.updateConnectorStatus(connector, ds);
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return updated
            ? UpdateControl.updateStatusSubResource(connector)
            : UpdateControl.noUpdate();
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

            this.eventSourceManager.registerEventSource(key, new WatcherEventSource<String>(kubernetesClient) {
                @SuppressWarnings("unchecked")
                @Override
                public void eventReceived(Action action, String resource) {
                    LOGGER.info("Event received for action: {}", action.name());

                    if (action == Action.ERROR) {
                        getLogger().warn("Skipping");
                        return;
                    }

                    try {
                        final Map<String, Object> unstructured = Serialization.jsonMapper().readValue(resource, Map.class);
                        final ResourceRef ref = ResourceUtil.asResourceRef(unstructured);
                        final ObjectMeta meta = ResourceUtil.getObjectMeta(unstructured);

                        //
                        // Since we need to know the owner UUID of the resource to properly
                        // generate the event, we can use the list of the owners
                        //
                        for (OwnerReference or : meta.getOwnerReferences()) {
                            eventHandler.handleEvent(
                                new ResourceEvent(action, ref, or.getUid(), this));
                        }
                    } catch (JsonProcessingException e) {
                        throw KubernetesClientException.launderThrowable(e);
                    }
                }

                @Override
                protected Watch watch() {
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

    private void extractStatus(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) throws Exception {
        if (connector.getStatus().getStatusExtractor() == null) {
            return;
        }

        var extractor = connector.getStatus().getStatusExtractor();

        LOGGER.info("Scraping status for resource {}/{}/{}",
            extractor.getRef().getApiVersion(),
            extractor.getRef().getKind(),
            extractor.getRef().getName());

        try {
            final ResourceRef ref = new ResourceRef(
                extractor.getRef().getApiVersion(),
                extractor.getRef().getKind(),
                extractor.getRef().getName());

            final JsonNode unstructured = uc.getAsNode(connector.getMetadata().getNamespace(), ref);

            if (!extractor.getConditions().getTypes().isEmpty()) {
                JsonNode conditions = unstructured.at(extractor.getConditions().getPath());
                if (!conditions.isMissingNode()) {
                    if (!conditions.isArray()) {
                        throw new IllegalArgumentException(
                            "Unsupported conditions field type: " + conditions.getNodeType());
                    }

                    for (JsonNode conditionNode : conditions) {
                        var condition = Serialization.jsonMapper().treeToValue(conditionNode, Condition.class);

                        for (String type : extractor.getConditions().getTypes()) {
                            if (!condition.getType().equals(type) && !condition.getType().matches(type)) {
                                continue;
                            }

                            var rc = new MetaV1Condition();
                            rc.setMessage(condition.getMessage());
                            rc.setReason(condition.getReason());
                            rc.setStatus(condition.getStatus());
                            rc.setType(condition.getType());
                            rc.setLastTransitionTime(condition.getLastTransitionTime());

                            LOGGER.info("Resource {}/{}/{} : extracted condition {}",
                                extractor.getRef().getApiVersion(),
                                extractor.getRef().getKind(),
                                extractor.getRef().getName(),
                                Serialization.asJson(condition));

                            deploymentStatus.addConditionsItem(rc);
                        }
                    }
                } else {
                    LOGGER.info("Resource {}/{}/{} does not have conditions",
                        extractor.getRef().getApiVersion(),
                        extractor.getRef().getKind(),
                        extractor.getRef().getName());
                }
            }

            if (!extractor.getPhase().getMapping().isEmpty()) {
                JsonNode phase = unstructured.at(extractor.getPhase().getPath());
                if (!phase.isMissingNode()) {
                    String phaseValue = phase.asText();

                    for (var mapping : extractor.getPhase().getMapping()) {
                        if (phase.equals(mapping.getResource()) || phaseValue.matches(mapping.getResource())) {
                            deploymentStatus.setPhase(mapping.getConnector());
                            break;
                        }
                    }

                } else {
                    LOGGER.info("Resource {}/{}/{} does not have phase",
                        extractor.getRef().getApiVersion(),
                        extractor.getRef().getKind(),
                        extractor.getRef().getName());
                }
            }
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            } else {
                LOGGER.info("Resource {}/{}/{} not found, skipping it",
                    extractor.getRef().getApiVersion(),
                    extractor.getRef().getKind(),
                    extractor.getRef().getName());
            }
        }
    }

    private void checkForAvailableUpdates(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        final List<Operator> operators = fleetShard.lookupOperators();
        final OperatorSelector selector = connector.getStatus().getDeployment().getOperatorSelector();

        selector.available(operators).ifPresent(
            operator -> {
                if (!Objects.equals(operator, connector.getStatus().getOperator())) {
                    LOGGER.info("deployment (upd): {} -> operator: {}",
                        connector.getSpec().getDeployment(),
                        operator);

                    deploymentStatus.setOperators(
                        new ConnectorDeploymentStatusOperators()
                            .assigned(new ConnectorOperator()
                                .id(connector.getStatus().getOperator().getId())
                                .type(connector.getStatus().getOperator().getType())
                                .version(connector.getStatus().getOperator().getVersion()))
                            .available(new ConnectorOperator()
                                .id(operator.getId())
                                .type(operator.getType())
                                .version(operator.getVersion())));
                } else {
                    deploymentStatus.setOperators(
                        new ConnectorDeploymentStatusOperators()
                            .assigned(new ConnectorOperator()
                                .id(connector.getStatus().getOperator().getId())
                                .type(connector.getStatus().getOperator().getType())
                                .version(connector.getStatus().getOperator().getVersion()))
                            .available(null));
                }
            });
    }
}
