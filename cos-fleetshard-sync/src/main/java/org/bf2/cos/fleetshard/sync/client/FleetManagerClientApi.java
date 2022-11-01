package org.bf2.cos.fleetshard.sync.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.api.ApiException;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeploymentStatus;

public interface FleetManagerClientApi {

    /**
     * Returns a list of connector deployments assigned to the cluster.
     */
    @GET
    @Path("/kafka_connector_clusters/{connector_cluster_id}/deployments/{deployment_id}")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorDeployment getConnectorDeploymentById(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId)
        throws ApiException, ProcessingException;

    /**
     * Returns a list of connector deployments assigned to the cluster.
     */
    @GET
    @Path("/kafka_connector_clusters/{connector_cluster_id}/deployments")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorDeploymentList getConnectorDeployments(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @QueryParam("page") String page,
        @QueryParam("size") String size,
        @QueryParam("gt_version") Long gtVersion)
        throws ApiException, ProcessingException;

    /**
     * Returns a connector namespace assigned to the cluster.
     */
    @GET
    @Path("/kafka_connector_clusters/{connector_cluster_id}/namespaces/{namespace_id}")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorNamespaceDeployment getConnectorNamespaceById(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("namespace_id") String namespaceId)
        throws ApiException, ProcessingException;

    /**
     * Returns all connector namespaces assigned to the cluster.
     */
    @GET
    @Path("/kafka_connector_clusters/{connector_cluster_id}/namespaces")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorNamespaceDeploymentList getConnectorNamespaces(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @QueryParam("page") String page,
        @QueryParam("size") String size,
        @QueryParam("gt_version") Long gtVersion)
        throws ApiException, ProcessingException;

    /**
     * Update the status of a connector deployment
     */
    @PUT
    @Path("/kafka_connector_clusters/{connector_cluster_id}/deployments/{deployment_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void updateConnectorDeploymentStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("deployment_id") String deploymentId,
        ConnectorDeploymentStatus connectorDeploymentStatus)
        throws ApiException, ProcessingException;

    /**
     * Update the status of a connector namespace
     *
     */
    @PUT
    @Path("/kafka_connector_clusters/{connector_cluster_id}/namespaces/{namespace_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void updateConnectorNamespaceStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        @PathParam("namespace_id") String namespaceId,
        ConnectorNamespaceDeploymentStatus connectorNamespaceStatus)
        throws ApiException, ProcessingException;

    /**
     * Update the status of a connector cluster
     */
    @PUT
    @Path("/kafka_connector_clusters/{connector_cluster_id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void updateConnectorClusterStatus(
        @PathParam("connector_cluster_id") String connectorClusterId,
        ConnectorClusterStatus connectorClusterStatus)
        throws ApiException, ProcessingException;
}
