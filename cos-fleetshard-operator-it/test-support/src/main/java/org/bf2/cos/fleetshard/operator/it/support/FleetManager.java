package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
@Path("/api/connector_mgmt/v1/kafka_connector_clusters/{connector_cluster_id}")
public class FleetManager {
    private final Map<String, ConnectorCluster> clusters;

    public FleetManager() {
        this.clusters = new ConcurrentHashMap<>();
    }

    @GET
    @Path("/deployments/{deployment_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorDeployment getConnectorDeployment(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId) {

        return getOrCreatCluster(connectorClusterId).getConnectorDeployments(deploymentId);
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

        return getOrCreatCluster(connectorClusterId).listConnectorDeployments();
    }

    @PUT
    @Path("/deployments/{deployment_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnectorDeploymentStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId,
        ConnectorDeploymentStatus connectorDeploymentStatus) {

        getOrCreatCluster(connectorClusterId).updateConnectorDeploymentStatus(deploymentId, connectorDeploymentStatus);
    }

    @PUT
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateClusterStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        ConnectorClusterStatus connectorClusterStatus) {

        getOrCreatCluster(connectorClusterId).updateConnectorClusterStatus(connectorClusterStatus);
    }

    // **********************************************
    //
    // Helper
    //
    // **********************************************

    public Optional<ConnectorCluster> getCluster(String id) {
        return Optional.ofNullable(clusters.get(id));
    }

    public ConnectorCluster getOrCreatCluster(String id) {
        return clusters.computeIfAbsent(id, ConnectorCluster::new);
    }

    // **********************************************
    //
    // Model
    //
    // **********************************************

    public static class ConnectorCluster {
        private final String id;
        private final Map<String, Connector> connectors;
        private ConnectorClusterStatus status;

        public ConnectorCluster(String id) {
            this.id = id;
            this.connectors = new ConcurrentHashMap<>();
            this.status = new ConnectorClusterStatus();
        }

        public Connector getConnector(String id) {
            return connectors.get(id);
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

        public ConnectorClusterStatus getStatus() {
            return status;
        }

        public void setConnectorDeployment(ConnectorDeployment deployment) {
            connectors.compute(deployment.getId(), (k, v) -> {
                if (v == null) {
                    v = new Connector();
                }
                v.deployment = deployment;
                return v;
            });
        }
    }

    public static class Connector {
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
