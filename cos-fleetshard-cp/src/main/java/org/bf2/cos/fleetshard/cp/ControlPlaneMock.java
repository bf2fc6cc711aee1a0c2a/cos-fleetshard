package org.bf2.cos.fleetshard.cp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnectorStatus;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnectorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
public class ControlPlaneMock {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlaneMock.class);

    private final ConnectorCluster cluster;
    private final Map<String, Connector> connectors;

    public ControlPlaneMock() {
        this.cluster = new ConnectorClusterBuilder().build();
        this.connectors = new HashMap<>();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorCluster getConnectorCluster(
            @PathParam("id") String id) {

        return cluster;
    }

    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnectorCluster(
            @PathParam("id") String id,
            ConnectorClusterStatus status) {

        this.cluster.setStatus(status);
    }

    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Connector> getConnectors(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion) {

        return Stream.concat(
                connectors.values().stream()
                        .filter(CamelConnector.class::isInstance)
                        .map(CamelConnector.class::cast)
                        .filter(c -> resourceVersion < c.getSpec().getResourceVersion()),
                connectors.values().stream()
                        .filter(DebeziumConnector.class::isInstance)
                        .map(DebeziumConnector.class::cast)
                        .filter(c -> resourceVersion < c.getSpec().getResourceVersion()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/connectors/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnector(
            @PathParam("id") String id,
            @PathParam("cid") String cid,
            ConnectorStatus status) {

        connectors.computeIfPresent(cid, (k, c) -> {
            if (c instanceof CamelConnector) {
                ((CamelConnector) c).setStatus((CamelConnectorStatus) status);
            } else if (c instanceof DebeziumConnector) {
                ((DebeziumConnector) c).setStatus((DebeziumConnectorStatus) status);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported connector/status connector: " + c.getClass() + ", status: " + status.getClass());
            }

            return c;
        });
    }

    @POST
    @Path("/{id}/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateConnector(
            String content)
            throws Exception {

        JsonNode node = new ObjectMapper().readTree(content);
        JsonNode kind = node.get("kind");
        if (kind == null) {
            throw new IllegalArgumentException("Unknown kind");
        }

        switch (kind.asText()) {
            case CamelConnector.KIND: {
                LOGGER.info("Got CamelConnector: {}", node);
                CamelConnector connector = Serialization.jsonMapper().treeToValue(node, CamelConnector.class);

                connectors.put(
                        connector.getMetadata().getAnnotations().get("cos.bf2.org/connector.id"),
                        connector);

                break;
            }
            case DebeziumConnector.KIND: {
                LOGGER.info("Got DebeziumConnector: {}", node);
                DebeziumConnector connector = Serialization.jsonMapper().treeToValue(node, DebeziumConnector.class);

                connectors.put(
                        connector.getMetadata().getAnnotations().get("cos.bf2.org/connector.id"),
                        connector);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown kind " + kind.asText());
        }
    }
}
