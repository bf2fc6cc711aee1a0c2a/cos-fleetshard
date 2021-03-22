package org.bf2.cos.fleetshard.operator.agent;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleetshard.api.Agent;
import org.bf2.cos.fleetshard.api.AgentStatus;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEventSource;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AgentController extends AbstractResourceController<Agent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentController.class);

    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        // set up a trigger to react to connector changes
        // TODO: is this needed ?
        eventSourceManager.registerEventSource(
                ConnectorEventSource.EVENT_SOURCE_ID,
                new ConnectorEventSource(kubernetesClient));
    }

    @Override
    public UpdateControl<Agent> createOrUpdateResource(
            Agent cluster,
            Context<Agent> context) {

        if (cluster.getStatus() == null) {
            cluster.setStatus(new AgentStatus());
        }
        if (!cluster.getStatus().isInPhase(AgentStatus.PhaseType.Ready)) {
            cluster.getStatus().setPhase(AgentStatus.PhaseType.Ready.name());
        }

        controlPlane.updateAgent(cluster);

        return UpdateControl.updateStatusSubResource(cluster);
    }
}
