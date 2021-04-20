package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.MetaV1Condition;
import org.bf2.cos.fleetshard.api.ConnectorStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.StatusExtractor;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.common.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.ResourceEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorController extends AbstractResourceController<ManagedConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;
    @Inject
    ControlPlane controlPlane;

    @Override
    public UpdateControl<ManagedConnector> createOrUpdateResource(
            ManagedConnector managedConnector,
            Context<ManagedConnector> context) {

        LOGGER.info("Reconcile {}/{}/{}",
                managedConnector.getApiVersion(),
                managedConnector.getKind(),
                managedConnector.getMetadata().getName());

        if (managedConnector.getStatus().isInPhase(ConnectorStatus.PhaseType.Provisioning)) {
            LOGGER.info("Connector is provisioning, skipping ...");
            return UpdateControl.noUpdate();
        }

        try {
            //
            // Set up watcher for resource types owned by the connectors. We don't
            // create a watch for each resource the connector owns to avoid creating
            // loads of watchers, instead we create a resource per type which will
            // triggers connectors based on the UUID of the owner (see the 'monitor'
            // method for more info)
            //
            for (ResourceRef resource : managedConnector.getStatus().getResources()) {
                monitor(context, managedConnector, resource);
            }

            setupResources(managedConnector);
            cleanupResources(managedConnector);

            // TODO: make this a little bit smart, for the moment always update the
            //       status with the latest info from resources and clean up every
            //       eventually orphaned resource
            managedConnector.getStatus().setPhase(ConnectorStatus.PhaseType.Provisioned);
            managedConnector.getStatus().setResources(managedConnector.getSpec().getResources());

            //
            // Don't report connector status till the connector is in "Provisioned"
            // state.
            //
            // TODO: this of course happens all the time at the moment, left here
            //       as an hint to improve the process
            //
            if (managedConnector.getStatus().isInPhase(ConnectorStatus.PhaseType.Provisioned)) {
                ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();

                for (StatusExtractor extractor : managedConnector.getSpec().getStatusExtractors()) {
                    LOGGER.info("Scraping status for resource {}/{}/{}",
                            extractor.getApiVersion(),
                            extractor.getKind(),
                            extractor.getName());

                    JsonNode unstructured = uc.getAsNode(managedConnector.getMetadata().getNamespace(), extractor);
                    JsonNode conditions = unstructured.at(extractor.getConditionsPath());

                    if (!conditions.isArray()) {
                        throw new IllegalArgumentException("Unsupported conditions field type: " + conditions.getNodeType());
                    }

                    for (JsonNode conditionNode : conditions) {
                        var condition = Serialization.jsonMapper().treeToValue(conditionNode, Condition.class);

                        var rc = new MetaV1Condition();
                        rc.setMessage(condition.getMessage());
                        rc.setReason(condition.getReason());
                        rc.setStatus(condition.getStatus());
                        rc.setType(condition.getType());
                        rc.setLastTransitionTime(condition.getLastTransitionTime());

                        ds.addConditionsItem(rc);
                    }
                }

                controlPlane.updateConnectorStatus(
                        managedConnector.getSpec().getClusterId(),
                        managedConnector.getMetadata().getName(),
                        ds);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return UpdateControl.updateStatusSubResource(managedConnector);
    }

    /**
     * Resource used by the connector need to have an owner ref point back to the
     * connector so when the connector gets deleted, those resource can be evicted
     * by kubernetes' gc.
     *
     * @param  managedConnector the connector
     * @throws IOException      in case of network/serialization failures or failures from Kubernetes API
     */
    private void setupResources(ManagedConnector managedConnector) throws IOException {
        for (ResourceRef ref : managedConnector.getSpec().getResources()) {
            final Map<String, Object> unstructured = uc.get(managedConnector.getMetadata().getNamespace(), ref);
            if (unstructured == null) {
                LOGGER.warn("Unable to find resource {}/{}/{}",
                        ref.getApiVersion(),
                        ref.getKind(),
                        ref.getName());

                return;
            }

            final ObjectMeta meta = ResourceUtil.getObjectMeta(unstructured);
            if (ResourceUtil.setOwnerReferences(meta, managedConnector)) {
                unstructured.put("metadata", meta);

                LOGGER.info(
                        "Set connector {}/{}/{} as owners of resource {}/{}/{}",
                        managedConnector.getApiVersion(),
                        managedConnector.getKind(),
                        managedConnector.getMetadata().getName(),
                        ref.getApiVersion(),
                        ref.getKind(),
                        ref.getName());

                uc.createOrReplace(managedConnector.getMetadata().getNamespace(), ref, unstructured);
            }
        }
    }

    /**
     * It may happen that the resources associated with a connector change between deployments,
     * as example for camel-k the secrets will be named after the resource version so we need
     * to remove potential orphaned resources.
     *
     * @param managedConnector the connector.
     */
    private void cleanupResources(ManagedConnector managedConnector) throws IOException {
        List<ResourceRef> toRemove = managedConnector.getStatus().getResources();
        toRemove.removeAll(managedConnector.getSpec().getResources());

        for (ResourceRef ref : toRemove) {
            LOGGER.info("Resource {}/{}/{} is not more required by connector {}/{}/{}, delete it",
                    ref.getApiVersion(),
                    ref.getKind(),
                    ref.getName(),
                    managedConnector.getApiVersion(),
                    managedConnector.getKind(),
                    managedConnector.getMetadata().getName());

            uc.delete(managedConnector.getMetadata().getNamespace(), ref);
        }
    }

    /**
     * As we do not know what resources we have to deal with so we can create a watcher per
     * for each kind + apiVersion combination.
     *
     * @param context          the context
     * @param managedConnector the connector that holds the resources
     * @param resource         the resource to watch
     */
    private synchronized void monitor(Context<ManagedConnector> context, ManagedConnector managedConnector,
            ResourceRef resource) {
        final EventSourceManager manager = context.getEventSourceManager();
        final String key = resource.getKind() + "@" + resource.getApiVersion();

        if (!manager.getRegisteredEventSources().containsKey(key)) {
            LOGGER.info("Registering an event for: {}", key);

            manager.registerEventSource(key, new ResourceEventSource(kubernetesClient) {
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
                protected void watch() {
                    uc.watch(managedConnector.getMetadata().getNamespace(), resource, this);
                }
            });
        }
    }
}
