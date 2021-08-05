package org.bf2.cos.fleetshard.operator.operand;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.bf2.cos.fleetshard.operator.support.ResourceEvent;
import org.bf2.cos.fleetshard.operator.support.WatcherEventSource;
import org.bf2.cos.fleetshard.support.resources.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_WATCH;
import static org.bf2.cos.fleetshard.support.resources.UnstructuredSupport.asCustomResourceDefinitionContext;

public class OperandResourceWatcher extends WatcherEventSource<GenericKubernetesResource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperandResourceWatcher.class);

    private final CustomResourceDefinitionContext context;
    private final String namespace;

    public OperandResourceWatcher(KubernetesClient client, String apiVersion, String kind) {
        this(client, asCustomResourceDefinitionContext(apiVersion, kind), null);
    }

    public OperandResourceWatcher(KubernetesClient client, String apiVersion, String kind, String namespace) {
        this(client, asCustomResourceDefinitionContext(apiVersion, kind), namespace);
    }

    public OperandResourceWatcher(KubernetesClient client, CustomResourceDefinitionContext context) {
        this(client, context, null);
    }

    public OperandResourceWatcher(KubernetesClient client, CustomResourceDefinitionContext context, String namespace) {
        super(client);

        this.context = context;
        this.namespace = namespace;
    }

    @Override
    protected Watch doWatch() {
        var client = getClient().genericKubernetesResources(context);
        if (this.namespace != null) {
            client.inNamespace(namespace);
        }

        return client.withLabel(LABEL_WATCH, "true").watch(this);
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
                ResourceUtil.asResourceRef(resource),
                resource.getMetadata().getOwnerReferences().get(0).getUid(),
                this));
    }
}
