package org.bf2.cos.fleetshard.operator.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.Agent;
import org.bf2.cos.fleetshard.api.AgentStatus;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorBuilder;
import org.bf2.cos.fleetshard.api.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.common.UnstructuredClient;
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
    @Inject
    UnstructuredClient uc;

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

    // ******************************************
    //
    // Control Plane Sync
    //
    // ******************************************

    @Scheduled(every = "{cos.agent.sync.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void pollConnectors() {
        LOGGER.debug("Polling for control plane managed connectors");
        getConnectorCluster().ifPresent(this::pollConnectors);
    }

    @SuppressWarnings("unchecked")
    private void pollConnectors(Agent connectorCluster) {
        for (ConnectorDeployment cd : controlPlane.getConnectors(connectorCluster)) {
            LOGGER.info("got {}", cd);

            List<ResourceRef> refs = new ArrayList<>();

            try {
                for (JsonNode node : cd.getSpec().getResources()) {
                    Map<String, Object> result = uc.createOrReplace(
                            connectorCluster.getMetadata().getNamespace(),
                            node,
                            Serialization.jsonMapper().treeToValue(node, Map.class));

                    refs.add(ResourceUtil.asResourceRef(result));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            kubernetesClient.customResources(Connector.class)
                    .inNamespace(connectorCluster.getMetadata().getNamespace())
                    .createOrReplace(
                            new ConnectorBuilder()
                                    .withMetadata(new ObjectMetaBuilder()
                                            .withName(cd.getId())
                                            .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                                            .build())
                                    .withSpec(new ConnectorSpecBuilder()
                                            .withConnectorResourceVersion(cd.getSpec().getResourceVersion())
                                            .withStatusExtractors(cd.getSpec().getStatusExtractors())
                                            .withResources(refs)
                                            .build())
                                    .build());
        }
    }

    private Optional<Agent> getConnectorCluster() {
        String namespace = kubernetesClient.getNamespace();

        KubernetesResourceList<Agent> items = kubernetesClient.customResources(Agent.class)
                .inNamespace(namespace)
                .list();

        if (items.getItems().isEmpty()) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }
        if (items.getItems().size() > 1) {
            // TODO: report the failure status to the CR and control plane
            throw new IllegalStateException("TODO");
        }

        Agent answer = items.getItems().get(0);
        if (answer.getStatus() == null || !answer.getStatus().isInPhase(AgentStatus.PhaseType.Ready)) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }

        return Optional.of(answer);
    }
}
