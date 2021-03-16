package org.bf2.cos.fleetshard.operator.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.operator.sync.cp.ControlPlaneClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorsSynchronizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsSynchronizer.class);

    @Inject
    @RestClient
    ControlPlaneClient controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "cos.agent.id", defaultValue = "cos")
    String agentId;

    /*
     * THIS IS A POC
     *
     * to sketch how the sync task should work, if the scheduler take more time than the
     * configured interval, the task is skipped
     */
    @Scheduled(every = "{cos.agent.sync.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void pollConnectors() {
        LOGGER.debug("Polling for control plane managed connectors");

        String namespace = kubernetesClient.getNamespace();

        getConnectorCluster().ifPresent(cc -> {

            pollConnectors(cc);
            // update the ConnectorCluster state to include the new resources
            // TODO: should the control plane also be updated ? in such case it should probably a task
            //       for the ConnectorCluster controller
            kubernetesClient
                    .customResources(ConnectorCluster.class)
                    .inNamespace(namespace)
                    .updateStatus(cc);
        });

    }

    private void pollConnectors(ConnectorCluster connectorCluster) {
        String namespace = kubernetesClient.getNamespace();

        for (Connector<?, ?> connector : getConnectors(connectorCluster)) {
            LOGGER.info("got {}", connector);

            if (connector instanceof CamelConnector) {
                kubernetesClient
                        .customResources(CamelConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace((CamelConnector) connector);
            } else if (connector instanceof DebeziumConnector) {
                kubernetesClient
                        .customResources(DebeziumConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace((DebeziumConnector) connector);
            } else {
                LOGGER.error("Unsupported connector type: {}", connector.getClass().getName());
                continue;
            }

            // TODO: update resource version
            connectorCluster
                    .getStatus()
                    .setResourceVersion(connector.getSpec().getResourceVersion());
        }
    }

    private Optional<ConnectorCluster> getConnectorCluster() {
        String namespace = kubernetesClient.getNamespace();

        KubernetesResourceList<ConnectorCluster> items = kubernetesClient.customResources(ConnectorCluster.class)
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

        ConnectorCluster answer = items.getItems().get(0);
        if (!answer.isReady()) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }

        return Optional.of(answer);
    }

    private List<Connector<?, ?>> getConnectors(ConnectorCluster connectorCluster) {
        final ArrayNode nodes = controlPlane.getConnectors(agentId, connectorCluster.getStatus().getResourceVersion());
        final List<Connector<?, ?>> answer = new ArrayList<>(nodes.size());

        if (nodes.isEmpty()) {
            LOGGER.info("No connectors with gv > {}", connectorCluster.getStatus().getResourceVersion());
            return Collections.emptyList();
        }

        try {
            for (JsonNode node : nodes) {
                JsonNode kind = node.get("kind");
                if (kind == null) {
                    continue;
                }

                switch (kind.asText()) {
                    case CamelConnector.KIND: {
                        LOGGER.info("Got CamelConnector: {}", node);
                        CamelConnector connector = Serialization.jsonMapper().treeToValue(node, CamelConnector.class);
                        answer.add(connector);
                        break;
                    }
                    case DebeziumConnector.KIND: {
                        LOGGER.info("Got DebeziumConnector: {}", node);
                        DebeziumConnector connector = Serialization.jsonMapper().treeToValue(node, DebeziumConnector.class);
                        answer.add(connector);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown kind " + kind.asText());
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to process connectors list", e);
        }

        answer.sort(Comparator.comparingLong(c -> c.getSpec().getResourceVersion()));

        return answer;
    }
}
