package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ControlPlane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlane.class);

    @Inject
    @RestClient
    ControlPlaneClient controlPlane;

    public void updateConnectorCluster(ConnectorCluster cluster) {
        controlPlane.updateConnectorCluster(cluster.getSpec().getConnectorClusterId(), cluster.getStatus());
    }

    public List<Connector> getConnectors(ConnectorCluster connectorCluster) {
        final ArrayNode nodes = controlPlane.getConnectors(connectorCluster.getSpec().getConnectorClusterId(),
                connectorCluster.getStatus().getResourceVersion());
        final List<Connector> answer = new ArrayList<>(nodes.size());

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

        return answer;
    }
}
