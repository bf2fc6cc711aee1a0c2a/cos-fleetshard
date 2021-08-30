package org.bf2.cos.fleetshard.operator.operand;

import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_WATCH;

import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.UnstructuredSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

public class OperandResourceWatcher extends WatcherEventSource<GenericKubernetesResource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperandResourceWatcher.class);

    private final ResourceDefinitionContext context;
    private final String namespace;

    public OperandResourceWatcher(KubernetesClient client, String apiVersion, String kind, String namespace) {
        this(client, UnstructuredSupport.asResourceDefinitionContext(apiVersion, kind), namespace);
    }

    public OperandResourceWatcher(KubernetesClient client, ResourceDefinitionContext context, String namespace) {
        super(client);

        this.context = context;
        this.namespace = namespace;
    }

    @Override
    protected Watch doWatch() {
        LOGGER.info("Watch resource {}/{}:{} on namespace {}",
            context.getVersion(),
            context.getGroup(),
            context.getKind(),
            namespace);

        if (this.namespace != null) {
            return getClient()
                .genericKubernetesResources(context)
                .inNamespace(namespace)
                .withLabel(LABEL_WATCH, "true")
                .watch(this);
        } else {
            return getClient()
                .genericKubernetesResources(context)
                .withLabel(LABEL_WATCH, "true")
                .watch(this);
        }
    }

    @Override
    protected void onEventReceived(Action action, GenericKubernetesResource resource) {
        if (resource.getMetadata().getOwnerReferences() == null) {
            LOGGER.debug("Ignoring action {} on resource {}:{}:{}@{} as OwnerReferences is missing",
                action,
                resource.getApiVersion(),
                resource.getKind(),
                resource.getMetadata().getName(),
                resource.getMetadata().getNamespace());

            return;
        }
        if (resource.getMetadata().getOwnerReferences().size() == 0) {
            LOGGER.debug("Ignoring action {} on resource {}:{}:{}@{} as it does not have OwnerReferences: {}",
                action,
                resource.getApiVersion(),
                resource.getKind(),
                resource.getMetadata().getName(),
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getOwnerReferences());

            return;
        }
        if (resource.getMetadata().getOwnerReferences().size() > 1) {
            LOGGER.debug("Ignoring action {} on resource {}:{}:{}@{} as it has multiple OwnerReferences: {}",
                action,
                resource.getApiVersion(),
                resource.getKind(),
                resource.getMetadata().getName(),
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getOwnerReferences());

            return;
        }

        LOGGER.debug("Event received on resource: {}/{}/{}@{}",
            resource.getApiVersion(),
            resource.getKind(),
            resource.getMetadata().getName(),
            resource.getMetadata().getNamespace());

        //
        // Since we need to know the owner UUID of the resource to properly
        // generate the event, we can use the list of the owners
        //
        getEventHandler().handleEvent(
            new ResourceEvent(
                action,
                Resources.asRef(resource),
                resource.getMetadata().getOwnerReferences().get(0).getUid(),
                this));
    }
}
