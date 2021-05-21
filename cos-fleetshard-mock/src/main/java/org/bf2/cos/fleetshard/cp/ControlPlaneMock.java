package org.bf2.cos.fleetshard.cp;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.api.model.cp.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;

@ApplicationScoped
@Path("/api/connector_mgmt/v1/kafka-connector-clusters/{connector_cluster_id}")
public class ControlPlaneMock {
    private final Map<String, ConnectorCluster> clusters;

    public ControlPlaneMock() {
        this.clusters = new ConcurrentHashMap<>();
    }

    @GET
    @Path("/deployments/{deployment_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeployment getConnectorDeployment(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId) {

        return cluster(connectorClusterId).getConnectorDeployments(deploymentId);
    }

    @GET
    @Path("/deployments")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeploymentList listConnectorDeployments(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @QueryParam("page") String page,
        @QueryParam("size") String size,
        @QueryParam("gt_version") Long gtVersion,
        @QueryParam("watch") String watch) {

        return cluster(connectorClusterId).listConnectorDeployments();
    }

    @PUT
    @Path("/deployments/{deployment_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnectorDeploymentStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId,
        ConnectorDeploymentStatus connectorDeploymentStatus) {

        cluster(connectorClusterId).updateConnectorDeploymentStatus(deploymentId, connectorDeploymentStatus);
    }

    @PUT
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateClusterStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        ConnectorClusterStatus connectorClusterStatus) {

        cluster(connectorClusterId).updateConnectorClusterStatus(connectorClusterStatus);
    }

    // **********************************************
    //
    // Helper
    //
    // **********************************************

    private ConnectorCluster cluster(String id) {
        return clusters.computeIfAbsent(id, ConnectorCluster::new);
    }

    // **********************************************
    //
    // Model
    //
    // **********************************************

    private static class ConnectorCluster {
        private final String id;
        private final Map<String, Connector> connectors;
        private ConnectorClusterStatus status;

        public ConnectorCluster(String id) {
            this.id = id;
            this.connectors = new ConcurrentHashMap<>();
        }

        public ConnectorDeployment getConnectorDeployments(String deploymentId) {
            return connectors.containsKey(deploymentId)
                ? connectors.get(deploymentId).getDeployment()
                : null;
        }

        public ConnectorDeploymentList listConnectorDeployments() {
            var items = connectors.values().stream()
                .map(Connector::getDeployment)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            return new ConnectorDeploymentList()
                .page(1)
                .size(items.size())
                .total(items.size())
                .items(items);
        }

        public void updateConnectorDeploymentStatus(String deploymentId, ConnectorDeploymentStatus connectorDeploymentStatus) {
            connectors.computeIfPresent(deploymentId, (k, v) -> {
                v.status = connectorDeploymentStatus;
                return v;
            });

        }

        public void updateConnectorClusterStatus(ConnectorClusterStatus connectorClusterStatus) {
            this.status = connectorClusterStatus;
        }
    }

    private static class Connector {
        private ConnectorDeployment deployment;
        private ConnectorDeploymentStatus status;

        public ConnectorDeployment getDeployment() {
            return deployment;
        }

        public void setDeployment(ConnectorDeployment deployment) {
            this.deployment = deployment;
        }

        public ConnectorDeploymentStatus getStatus() {
            return status;
        }

        public void setStatus(ConnectorDeploymentStatus status) {
            this.status = status;
        }
    }
}
