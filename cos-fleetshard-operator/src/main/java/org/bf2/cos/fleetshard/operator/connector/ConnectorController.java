package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.zjsonpatch.JsonDiff;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleet.manager.api.model.cp.MetaV1Condition;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentReifyRequest;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentSpec;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.common.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.PodEventSource;
import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.SecretEventSource;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_ANNOTATION;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;

@Controller(
    name = "connector")
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    private final Set<String> events;
    private final TimerEventSource retryTimer;

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;
    @Inject
    ControlPlane controlPlane;

    public ConnectorController() {
        this.events = new HashSet<>();
        this.retryTimer = new TimerEventSource();
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_connector-retry-timer",
            retryTimer);

        eventSourceManager.registerEventSource(
            "_connector-secrets",
            new SecretEventSource(
                kubernetesClient,
                List.of(
                    ManagedConnector.LABEL_CONNECTOR_ID,
                    ManagedConnector.LABEL_DEPLOYMENT_ID)));

        eventSourceManager.registerEventSource(
            "_connector-meta-pods",
            new PodEventSource(
                kubernetesClient,
                List.of(
                    ManagedConnector.LABEL_CONNECTOR_ID,
                    ManagedConnector.LABEL_DEPLOYMENT_ID,
                    ManagedConnector.LABEL_CONNECTOR_META)));
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
            return handleInitialization(context, connector);
        }

        switch (connector.getStatus().getPhase()) {
            case Initialization:
                return handleInitialization(context, connector);
            case EphemeralMeta:
                return handleEphemeralMeta(context, connector);
            case EphemeralMetaWatch:
                return handleEphemeralMetaWatch(context, connector);
            case Augmentation:
                return handleAugmentation(context, connector);
            case Monitor:
                return handleMonitor(context, connector);
            default:
                throw new UnsupportedOperationException("Unsupported phase: " + connector.getStatus().getPhase());
        }

        // TODO:
        //   delete orphaned resources when done (including the secret create by the sink)
    }

    // **************************************************
    //
    // Handlers
    //
    // **************************************************

    private UpdateControl<ManagedConnector> handleInitialization(
        Context<ManagedConnector> context,
        ManagedConnector connector) {

        // TODO: add helper method to update connector phase
        try {
            controlPlane.updateConnectorStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                new ConnectorDeploymentStatus().phase("provisioning"));

        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (connector.getSpec().getDeployment().getDesiredState()) {
            case DESIRED_STATE_DELETED: {
                connector.getStatus().setDeployment(connector.getSpec().getDeployment());
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);
                break;
            }
            case DESIRED_STATE_READY: {
                if (Strings.isNullOrEmpty(connector.getSpec().getDeployment().getMetaServiceHost())) {
                    connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.EphemeralMeta);
                } else {
                    connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Augmentation);
                }
                break;
            }
            default:
                throw new IllegalStateException(
                    "Unknown desired state: " + connector.getSpec().getDeployment().getDesiredState());
        }

        return UpdateControl.updateStatusSubResource(connector);
    }

    /*
     * This phase is about creating the ephemeral meta pod.
     */
    private UpdateControl<ManagedConnector> handleEphemeralMeta(
        Context<ManagedConnector> context,
        ManagedConnector connector) {

        LOGGER.info("Spinning up Meta Service");

        final Pod pod = ConnectorSupport.createMetaPod(connector);
        final Service service = ConnectorSupport.createMetaPodService(connector);

        kubernetesClient.pods()
            .inNamespace(connector.getMetadata().getNamespace())
            .create(pod);
        kubernetesClient.services()
            .inNamespace(connector.getMetadata().getNamespace())
            .create(service);

        connector.getStatus().addCondition(
            ManagedConnectorStatus.ConditionType.MetaPodCreated,
            ManagedConnectorStatus.ConditionStatus.True,
            "Pod " + pod.getMetadata().getName() + " created");
        connector.getStatus().addCondition(
            ManagedConnectorStatus.ConditionType.MetaPodServiceCreated,
            ManagedConnectorStatus.ConditionStatus.True,
            "Service " + service.getMetadata().getName() + " created");

        connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.EphemeralMetaWatch);
        connector.getStatus().addResource(pod);
        connector.getStatus().addResource(service);

        return UpdateControl.updateStatusSubResource(connector);
    }

    /*
     * This phase is about waiting for the ephemeral meta pod to be ready.
     */
    private UpdateControl<ManagedConnector> handleEphemeralMetaWatch(
        Context<ManagedConnector> context,
        ManagedConnector connector) {

        final DeploymentSpec ref = connector.getSpec().getDeployment();
        final String connectorId = connector.getMetadata().getName();
        final String name = connectorId + "-" + ref.getDeploymentResourceVersion();

        Pod pod = kubernetesClient.pods()
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(name)
            .get();

        if (pod == null) {
            throw new IllegalStateException("Pod " + name + " does not exists");
        }

        switch (pod.getStatus().getPhase()) {
            case "Pending": {
                LOGGER.info("Meta Pod creation pending");
                return UpdateControl.noUpdate();
            }
            case "Running": {
                LOGGER.info("Meta Pod ready");
                connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Augmentation);
                return UpdateControl.updateStatusSubResource(connector);
            }
            default: {
                LOGGER.warn("Meta Pod {}", pod.getStatus().getReason());
                return UpdateControl.noUpdate();
            }
        }
    }

    /*
     * In this phase the generic connector definition is reified and the custom resources for the
     * downstream operators are created.
     */
    @SuppressWarnings("unchecked")
    private UpdateControl<ManagedConnector> handleAugmentation(
        Context<ManagedConnector> context,
        ManagedConnector connector) {

        final DeploymentSpec ref = connector.getSpec().getDeployment();
        final String connectorId = connector.getMetadata().getName();
        final String name = connectorId + "-" + ref.getDeploymentResourceVersion();

        // TODO: we can have a shorter prefix since the service is not exposed anymore to the control plane
        // TODO: requires something like localizer (https://github.com/getoutreach/localizer) for local testing
        final String fmt = "http://%s/reify";
        final String host = ref.getMetaServiceHost() != null ? ref.getMetaServiceHost() : name;
        final String url = String.format(fmt, host, connector.getSpec().getConnectorTypeId());

        LOGGER.info("Connecting to meta service at : {}", url);

        try {
            ConnectorDeployment deployment = controlPlane.getDeployment(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId());

            ConnectorDeploymentReifyRequest rr = new ConnectorDeploymentReifyRequest();
            rr.setConnectorResourceVersion(deployment.getSpec().getConnectorResourceVersion());
            rr.setDeploymentResourceVersion(ref.getDeploymentResourceVersion());
            rr.setManagedConnectorId(connectorId);
            rr.setDeploymentId(connector.getSpec().getDeploymentId());
            rr.setConnectorId(deployment.getSpec().getConnectorId());
            rr.setConnectorId(deployment.getSpec().getConnectorTypeId());
            rr.setConnectorSpec(deployment.getSpec().getConnectorSpec());
            rr.setShardMetadata(deployment.getSpec().getShardMetadata());

            //rr.setKafkaId(ref.getKafkaId());

            // TODO: replace with a proper client
            // TODO: set-up ssl
            var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Serialization.asJson(rr)))
                .build();

            final var ns = connector.getMetadata().getNamespace();
            final var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            final var cds = Serialization.unmarshal(response.body(), ConnectorDeploymentSpec.class);

            if (cds.getResources() != null) {
                for (JsonNode node : cds.getResources()) {
                    LOGGER.info("Got {}", Serialization.asJson(node));

                    ObjectNode on = (ObjectNode) node;
                    on.with("metadata")
                        .withArray("ownerReferences")
                        .addObject()
                        .put("apiVersion", connector.getApiVersion())
                        .put("controller", true)
                        .put("kind", connector.getKind())
                        .put("name", connector.getMetadata().getName())
                        .put("uid", connector.getMetadata().getUid());

                    final var resource = uc.createOrReplace(ns, node);
                    final var meta = (Map<String, Object>) resource.getOrDefault("metadata", Map.of());
                    final var annotations = (Map<String, String>) meta.get("annotations");
                    final var rApiVersion = (String) resource.get("apiVersion");
                    final var rKind = (String) resource.get("kind");
                    final var rName = (String) meta.get("name");
                    final var res = new DeployedResource(rApiVersion, rKind, rName);

                    if (DELETION_MODE_CONNECTOR.equals(annotations.get(DELETION_MODE_ANNOTATION))) {
                        if (!connector.getStatus().getResources().contains(res)) {
                            connector.getStatus().getResources().add(res);
                        }
                    } else {
                        res.setDeploymentRevision(ref.getDeploymentResourceVersion());
                        connector.getStatus().getResources().add(res);
                    }

                    //
                    // Set up watcher for resource types owned by the connectors. We don't
                    // create a watch for each resource the connector owns to avoid creating
                    // loads of watchers, instead we create a resource per type which will
                    // triggers connectors based on the UUID of the owner (see the 'monitor'
                    // method for more info)
                    //
                    watchResource(context, connector, res);
                }
            }

            connector.getStatus().setStatusExtractor(cds.getStatusExtractor());
            connector.getStatus().setDeployment(connector.getSpec().getDeployment());
            connector.getStatus().setPhase(ManagedConnectorStatus.PhaseType.Monitor);

            return UpdateControl.updateStatusSubResource(connector);
        } catch (ConnectException e) {
            LOGGER.warn("Error connecting to meta service " + url + ", retrying");
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

    /*
     * The connector has been provisioned and this phase is about:
     * - monitoring the status of the resources created for the downstream operator and report it to the control plane
     * - perform cleanup logic
     * - perform upgrade checks
     */
    private UpdateControl<ManagedConnector> handleMonitor(
        Context<ManagedConnector> context,
        ManagedConnector connector) {

        // TODO: check available operators

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
            ds.setConditions(new ArrayList<>());

            if (connector.getStatus().getStatusExtractor() != null) {
                var extractor = connector.getStatus().getStatusExtractor();

                LOGGER.info("Scraping status for resource {}/{}/{}",
                    extractor.getRef().getApiVersion(),
                    extractor.getRef().getKind(),
                    extractor.getRef().getName());

                try {
                    final ResourceRef ref = new ResourceRef(extractor.getRef().getApiVersion(), extractor.getRef().getKind(),
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

                                    LOGGER.warn("Resource {}/{}/{} : extracted condition {}",
                                        extractor.getRef().getApiVersion(),
                                        extractor.getRef().getKind(),
                                        extractor.getRef().getName(),
                                        Serialization.asJson(condition));

                                    ds.addConditionsItem(rc);
                                }
                            }
                        } else {
                            LOGGER.warn("Resource {}/{}/{} does not have conditions",
                                extractor.getRef().getApiVersion(),
                                extractor.getRef().getKind(),
                                extractor.getRef().getName());
                        }
                    }

                    if (!extractor.getPhase().getMapping().isEmpty()) {
                        JsonNode phase = unstructured.at(extractor.getPhase().getPath());
                        if (!phase.isMissingNode()) {
                            String phaseValue = phase.asText();

                            for (var x : extractor.getPhase().getMapping()) {
                                if (phase.equals(x.getResource()) || phaseValue.matches(x.getResource())) {
                                    ds.setPhase(x.getConnector());
                                    break;
                                }
                            }

                        } else {
                            LOGGER.warn("Resource {}/{}/{} does not have phase",
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

            controlPlane.updateConnectorStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                ds);

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
     * @param context   the context
     * @param connector the connector that holds the resources
     * @param resource  the resource to watch
     */
    private synchronized void watchResource(Context<ManagedConnector> context, ManagedConnector connector,
        ResourceRef resource) {
        if ("Secret".equals(resource.getKind()) && "v1".equals(resource.getApiVersion())) {
            LOGGER.info("Skip registering event source for secret");
            return;
        }

        final EventSourceManager manager = context.getEventSourceManager();
        final String key = resource.getApiVersion() + ":" + resource.getKind();

        if (this.events.add(key)) {
            LOGGER.info("Registering an event for: {}", key);

            manager.registerEventSource(key, new WatcherEventSource<String>(kubernetesClient) {
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
                    return uc.watch(connector.getMetadata().getNamespace(), resource, this);
                }
            });
        }
    }
}
