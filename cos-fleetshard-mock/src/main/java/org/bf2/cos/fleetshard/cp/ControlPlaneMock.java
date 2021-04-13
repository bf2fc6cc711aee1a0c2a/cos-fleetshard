package org.bf2.cos.fleetshard.cp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.api.model.ConnectorCluster;
import org.bf2.cos.fleet.manager.api.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.ConnectorDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.client.utils.Serialization;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
public class ControlPlaneMock {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlaneMock.class);

    private final ConnectorCluster cluster;
    private final AtomicLong counter;
    private final Map<String, ConnectorDeployment> connectors;

    public ControlPlaneMock() {
        this.cluster = new ConnectorCluster();
        this.counter = new AtomicLong();
        this.connectors = new HashMap<>();
    }

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(args);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorCluster getConnectorCluster(
            @PathParam("id") String id) {

        return cluster;
    }

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateKafkaConnectorClusterStatus(
            @PathParam("id") String id,
            ConnectorClusterStatus status) {

        this.cluster.setStatus(status.getPhase());
    }

    @GET
    @Path("/{id}/deployments")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeploymentList getDeployments(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion) {

        List<ConnectorDeployment> deployments = connectors.values().stream()
            .filter(c -> resourceVersion < c.getMetadata().getResourceVersion())
            .sorted(Comparator.comparingLong(c -> c.getMetadata().getResourceVersion()))
            .collect(Collectors.toList());

        return new ConnectorDeploymentList()
                .total(deployments.size())
                .page(0)
                .items(deployments);
    }

    @PUT
    @Path("/{id}/deployments/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnector(
            @PathParam("id") String id,
            @PathParam("cid") String cid,
            ConnectorDeploymentStatus status) throws JsonProcessingException {

        LOGGER.info("Updating status {}",
                Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(status));

        connectors.computeIfPresent(
                cid,
                (k, v) -> {
                    v.setStatus(status);
                    return v;
                });
    }

    @POST
    @Path("/{id}/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateConnector(
            ConnectorDeployment connector) {

        connector.getMetadata().setResourceVersion(counter.incrementAndGet());
        connectors.put(connector.getId(), connector);
    }
}
