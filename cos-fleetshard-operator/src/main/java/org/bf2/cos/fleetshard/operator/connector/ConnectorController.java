package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorStatus;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.common.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;
import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.ResourceEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorController extends AbstractResourceController<Connector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;

    @Override
    public UpdateControl<Connector> createOrUpdateResource(
            Connector connector,
            Context<Connector> context) {

        if (connector.getStatus() != null) {
            // Set up watcher for resources created by the connector
            for (ResourceRef resource : connector.getStatus().getResources()) {
                // TODO: implement
                watch(context, connector, resource);
            }
        }

        context.getEvents().getLatestOfType(DependantResourceEvent.class).ifPresent(e -> {
            if (connector.getSpec().shouldStatusBeExtracted(e.getObjectReference())) {
                // TODO: implement
            }
        });

        // TODO: lookup target namespace
        // TODO: create resources cd.spec.resources
        // TODO: set owner ref
        // TODO: check resource version
        // TODO: create resource on k8s in the target namespace

        if (connector.getStatus() == null) {
            connector.setStatus(new ConnectorStatus());
        }

        try {
            setupResources(connector);
            cleanupResources(connector);

            connector.getStatus().setResources(
                    connector.getSpec().getResources());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return UpdateControl.updateStatusSubResource(connector);
    }

    /**
     * Resource used by the connector need to have an owner ref point back to the
     * connector so when the connector gets deleted, those resource can be evicted
     * by kubernetes' gc.
     *
     * @param  connector   the connector
     * @throws IOException in case of network/serializiation failures or failures from Kuberntes API
     */
    private void setupResources(Connector connector) throws IOException {
        for (ResourceRef ref : connector.getSpec().getResources()) {
            final Map<String, Object> unstructured = uc.get(connector.getMetadata().getNamespace(), ref);
            final ObjectMeta meta = ResourceUtil.getObjectMeta(unstructured);

            ResourceUtil.addOwnerReferences(meta, connector);

            unstructured.put("metadata", meta);

            LOGGER.info(
                    "Add connector {}/{}/{} among owners of resource {}/{}/{}, refs={}",
                    connector.getApiVersion(),
                    connector.getKind(),
                    connector.getMetadata().getName(),
                    ref.getApiVersion(),
                    ref.getKind(),
                    ref.getName(),
                    meta.getOwnerReferences().size());

            LOGGER.info(
                    "result: {}",
                    Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                            uc.createOrReplace(connector.getMetadata().getNamespace(), ref, unstructured)));
        }
    }

    /**
     * It may happen that the resources associated with a connector change between deployments,
     * as example for camel-k the secrets will be named after the resource version so we need
     * to remove potential orphaned resources.
     *
     * @param connector the connector.
     */
    private void cleanupResources(Connector connector) {
        List<ResourceRef> toRemove = connector.getStatus().getResources();
        toRemove.removeAll(connector.getSpec().getResources());

        for (ResourceRef ref : toRemove) {
            LOGGER.info("Resource {}/{}/{} is not more required by connector {}/{}/{}, remove owner ref",
                    ref.getApiVersion(),
                    ref.getKind(),
                    ref.getName(),
                    connector.getApiVersion(),
                    connector.getKind(),
                    connector.getMetadata().getName());

            final Map<String, Object> unstructured = uc.get(connector.getMetadata().getNamespace(), ref);
            final ObjectMeta meta = ResourceUtil.getObjectMeta(unstructured);

            //
            // Since the current connector does not need this resource anymore, we should remove
            // the connector from the owner references.
            //
            ResourceUtil.removeOwnerReferences(meta, connector);

            //
            // Resource may be shared among containers, as example camel-k kamelets, so they must
            // not be removed if additional owners are listed.
            //
            if (meta.getOwnerReferences().isEmpty()) {
                LOGGER.info("Resource {}/{}/{} is not more required by connector {}/{}/{} and no other refs, removing it.",
                        ref.getApiVersion(),
                        ref.getKind(),
                        ref.getName(),
                        connector.getApiVersion(),
                        connector.getKind(),
                        connector.getMetadata().getName());

                uc.delete(connector.getMetadata().getNamespace(), ref);
            }
        }
    }

    /**
     * As we do not know what resources we have to deal with so we can create a watcher per
     * for each kind + apiVersion combination.
     *
     * @param context   the context
     * @param connector the connector that holds the resources
     * @param resource  the resource to watch
     */
    private void watch(Context<Connector> context, Connector connector, ResourceRef resource) {
        final EventSourceManager manager = context.getEventSourceManager();
        final String key = resource.getKind() + "@" + resource.getApiVersion();

        if (!manager.getRegisteredEventSources().containsKey(key)) {
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
                        // TODO: check if the UUID is really needed.
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
                    uc.watch(connector.getMetadata().getNamespace(), resource, this);
                }
            });
        }
    }
}
