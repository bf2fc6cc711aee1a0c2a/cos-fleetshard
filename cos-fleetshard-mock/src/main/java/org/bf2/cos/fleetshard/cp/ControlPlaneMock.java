package org.bf2.cos.fleetshard.cp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

import org.bf2.cos.fleet.manager.api.model.cp.ConnectorCluster;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
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
    @Path("/{cluster_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorCluster getConnectorCluster(
        @PathParam("id") String clusterId) {

        return cluster;
    }

    @PUT
    @Path("/{cluster_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateKafkaConnectorClusterStatus(
        @PathParam("cluster_id") String clusterId,
        ConnectorClusterStatus status) {

        this.cluster.setStatus(status.getPhase());
    }

    @GET
    @Path("/{cluster_id}/deployments")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeploymentList getDeployments(
        @PathParam("cluster_id") String clusterId,
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

    @GET
    @Path("/{cluster_id}/connectors")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeploymentList getConnectors(
        @PathParam("cluster_id") String clusterId) {

        List<ConnectorDeployment> deployments = connectors.values().stream()
            .sorted(Comparator.comparingLong(c -> c.getMetadata().getResourceVersion()))
            .collect(Collectors.toList());

        return new ConnectorDeploymentList()
            .total(deployments.size())
            .page(0)
            .items(deployments);
    }

    @GET
    @Path("/{id}/deployments/{deployment_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeployment getDeployment(
        @PathParam("cluster_id") String clusterId,
        @PathParam("deployment_id") String deploymentId) {

        return connectors.values().stream()
            .filter(cd -> Objects.equals(deploymentId, cd.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown deployment " + deploymentId));
    }

    @PUT
    @Path("/{cluster_id}/deployments/{deployment_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnector(
        @PathParam("cluster_id") String clusterId,
        @PathParam("deployment_id") String deploymentId,
        ConnectorDeploymentStatus status) throws JsonProcessingException {

        LOGGER.info("Updating status {} -> {}",
            deploymentId,
            Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(status));

        connectors.values().stream()
            .filter(cd -> Objects.equals(deploymentId, cd.getId()))
            .findFirst()
            .ifPresent(v -> v.setStatus(status));
    }

    @POST
    @Path("/{cluster_id}/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateConnector(
        ConnectorDeployment connector) throws JsonProcessingException {

        String id = connector.getId();
        long rev = counter.incrementAndGet();
        connector.setId(UUID.randomUUID().toString());
        connector.getMetadata().setResourceVersion(rev);
        connector.getSpec().setConnectorResourceVersion(rev);

        LOGGER.info("Updating deployment {} ->{}",
            id,
            Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(connector));

        connectors.put(id, connector);
    }
}
