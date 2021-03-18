package org.bf2.cos.fleetshard.operator.connector;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorController extends AbstractResourceController<Connector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorController.class);

    @Inject
    KubernetesClient client;

    @Override
    public UpdateControl<Connector> createOrUpdateResource(
            Connector connector,
            Context<Connector> context) {

        if (connector.getStatus() != null) {
            // Set up watcher for resources created by the connector
            for (ObjectReference resource : connector.getStatus().getResources()) {
                // TODO: implement
                //watch(context, resource);
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

        try {
            LOGGER.info("createOrUpdateResource spec: {}",
                    connector.getSpec());
            LOGGER.info("createOrUpdateResource spec: {}",
                    Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(connector.getSpec()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return UpdateControl.noUpdate();
    }

    private void watch(Context<Connector> context, ObjectReference resource) {
        final EventSourceManager manager = context.getEventSourceManager();
        final String key = resource.getKind() + "@" + resource.getApiVersion();

        if (!manager.getRegisteredEventSources().containsKey(key)) {
            manager.registerEventSource(key, new DependantResourceEventSource<>(client) {
                @Override
                public void eventReceived(Action action, HasMetadata resource) {
                    LOGGER.info("Event received for action: {}", action.name());
                    if (action == Action.ERROR) {
                        getLogger().warn("Skipping");
                        return;
                    }

                    eventHandler.handleEvent(
                            new DependantResourceEvent(action, resource, this));
                }

                @Override
                protected void watch() {
                    //client.re
                    // TODO: set up watcher
                }
            });
        }
    }
}
