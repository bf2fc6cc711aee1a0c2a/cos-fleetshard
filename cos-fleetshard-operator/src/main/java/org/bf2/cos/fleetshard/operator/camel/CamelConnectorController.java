package org.bf2.cos.fleetshard.operator.camel;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEvent;
import org.bf2.cos.fleetshard.operator.support.DependantResourceEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class CamelConnectorController implements ResourceController<CamelConnector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelConnectorController.class);

    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
                KameletBindingEventSource.EVENT_SOURCE_ID,
                new KameletBindingEventSource(client));
    }

    @Override
    public UpdateControl<CamelConnector> createOrUpdateResource(
            CamelConnector connector,
            Context<CamelConnector> context) {

        LOGGER.info("createOrUpdateResource {}", connector);
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(
            CamelConnector connector,
            Context<CamelConnector> context) {

        LOGGER.info("deleteResource {}", connector);
        return DeleteControl.DEFAULT_DELETE;
    }

    // *************************************************************************
    //
    // KameletBinding (example)
    //
    // TODO: use official k8s client extension when available
    //       https://github.com/fabric8io/kubernetes-client/pull/2891
    //
    // *************************************************************************

    public static class KameletBinding {
    }

    public static class KameletBindingEventSource extends DependantResourceEventSource<KameletBinding> {
        public static String EVENT_SOURCE_ID = "kamelet-binding-event-source";

        public KameletBindingEventSource(KubernetesClient client) {
            super(client);
        }

        @Override
        protected void watch() {
            //getClient().apps().deployments().inAnyNamespace().withLabels(labels).watch(this);
        }

        @Override
        public void eventReceived(Action action, KameletBinding resource) {
            LOGGER.info("Event received for action: {}", action.name());
            if (action == Action.ERROR) {
                LOGGER.warn("Skipping");
                return;
            }

            eventHandler.handleEvent(
                    new KameletBindingEvent(
                            action,
                            resource,
                            "",
                            this));
        }
    }

    public static class KameletBindingEvent extends DependantResourceEvent<KameletBinding> {
        public KameletBindingEvent(
                Watcher.Action action,
                KameletBinding resource,
                String ownerUid,
                KameletBindingEventSource eventSource) {
            super(action, resource, ownerUid, eventSource);
        }

        @Override
        public String toString() {
            return "KameletBindingEvent{}";
        }
    }

}
