package org.bf2.cos.fleetshard.cp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnectorStatus;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnectorStatus;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
public class ControlPlaneMock {
    private final ConnectorCluster cluster;
    private final List<Connector<?, ?>> connectors;

    public ControlPlaneMock() {
        this.cluster = new ConnectorClusterBuilder()
        this.connectors = new ArrayList<>();
    }

    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnectorCluster(String id, ConnectorClusterStatus status) {
        this.cluster.setStatus(status);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorCluster getConnectorCluster(String id) {
        return cluster;
    }

    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Connector<?, ?>> getConnectors(String id, long resourceVersion) {
        return connectors.stream()
                .filter(c -> resourceVersion < c.getSpec().getResourceVersion())
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/connectors/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnector(String id, String cid, ConnectorStatus status) {
        connectors.stream()
                .filter(c -> Objects.equals(cid, c.getMetadata().getAnnotations().get("cos.bf2.org/connector.id")))
                .findFirst()
                .ifPresent(c -> {
                    if (c instanceof CamelConnector) {
                        ((CamelConnector) c).setStatus((CamelConnectorStatus) status);
                    } else if (c instanceof DebeziumConnector) {
                        ((DebeziumConnector) c).setStatus((DebeziumConnectorStatus) status);
                    } else {
                        throw new IllegalArgumentException(
                                "Unsupported connector/status connector: " + c.getClass() + ", status: " + status.getClass());
                    }
                });
    }
}
