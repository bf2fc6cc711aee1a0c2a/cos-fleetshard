package org.bf2.cos.fleetshard.operator.connector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.Agent;
import org.bf2.cos.fleetshard.api.AgentStatus;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorDeployment;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.common.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;

/**
 * Implements the synchronization protocol for the connectors.
 */
@ApplicationScoped
public class ConnectorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSync.class);

    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    UnstructuredClient uc;

    @Scheduled(every = "{cos.connectors.sync.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.debug("Sync connectors");

        String namespace = kubernetesClient.getNamespace();

        KubernetesResourceList<Agent> items = kubernetesClient.customResources(Agent.class).inNamespace(namespace).list();

        if (items.getItems().isEmpty()) {
            LOGGER.debug("Agent not yet configured");
            return;
        }

        if (items.getItems().size() > 1) {
            // TODO: report the failure status to the CR and control plane
            LOGGER.warn("More than one Agent");
        }

        Agent agent = items.getItems().get(0);
        if (agent.getStatus() == null || !agent.getStatus().isInPhase(AgentStatus.PhaseType.Ready)) {
            LOGGER.debug("Agent not yet configured");
            return;
        }

        LOGGER.debug("Polling for control plane connectors");

        List<ConnectorDeployment> deployments = controlPlane.getConnectors(agent);
        if (deployments.isEmpty()) {
            LOGGER.info("No connectors for agent {}", agent.getMetadata().getName());
        }

        for (var entry : deployments.stream().collect(groupingBy(ConnectorDeployment::getId)).entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(c -> c.getSpec().getResourceVersion()));

            //
            // in a single poll cycle, we may get multiple revision for the same connector in case
            // the user updates it during the poll interval so we should filter out any intermediate
            // revision.
            //
            for (int i = 0; i < entry.getValue().size() - 1; i++) {
                LOGGER.info("skip intermediate connector deployment (id={}, resource_version={})",
                        entry.getKey(),
                        entry.getValue().get(i).getSpec().getResourceVersion());
            }

            try {
                provision(
                        agent,
                        entry.getValue().get(entry.getValue().size() - 1));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void provision(
            Agent agent,
            ConnectorDeployment deployment)
            throws Exception {

        LOGGER.info("deploying connector {}, {}", deployment.getId(), deployment.getSpec());

        Connector connector = kubernetesClient.customResources(Connector.class)
                .inNamespace(agent.getMetadata().getNamespace())
                .withName(deployment.getId())
                .get();

        if (connector != null) {
            //
            // If the connector resource version is greater than the resource version of the deployment
            // request, skip it as we assume nothing has changed.
            //
            // TODO: this should not happen as the connector poll procedure should automatically filter
            //       out any unwanted resources so we may want to remove this once the system is proven
            //       to be stable enough.
            //
            if (connector.getSpec().getConnectorResourceVersion() > deployment.getSpec().getResourceVersion()) {
                return;
            }
        } else {
            connector = new Connector();
            connector.getMetadata().setName(deployment.getId());
            connector.getMetadata().setOwnerReferences(List.of(ResourceUtil.asOwnerReference(agent)));
            connector.getSpec().setAgentId(agent.getSpec().getAgentId());
        }

        connector.getSpec().setConnectorResourceVersion(deployment.getSpec().getResourceVersion());
        connector.getSpec().setStatusExtractors(deployment.getSpec().getStatusExtractors());
        connector.getSpec().setResources(new ArrayList<>());

        for (JsonNode node : deployment.getSpec().getResources()) {
            Map<String, Object> result = uc.createOrReplace(
                    agent.getMetadata().getNamespace(),
                    node);

            connector.getSpec().getResources().add(ResourceUtil.asResourceRef(result));
        }

        kubernetesClient.customResources(Connector.class)
                .inNamespace(agent.getMetadata().getNamespace())
                .createOrReplace(connector);

    }
}
