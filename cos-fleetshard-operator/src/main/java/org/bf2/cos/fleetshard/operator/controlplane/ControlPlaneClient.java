package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorCluster;
import org.bf2.cos.fleetshard.api.ConnectorDeployment;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@Path(ControlPlaneClient.BASE_PATH)
@RegisterRestClient(configKey = ControlPlaneClient.CONFIG_KEY)
public interface ControlPlaneClient {
    String CONFIG_KEY = "control-plane";
    String BASE_PATH = "/api/managed-services-api/v1/kafka-connector-clusters";

    /**
     * Updates the status of the agent.
     *
     * @param id     the id of the cluster
     * @param status the status of the cluster
     */
    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorCluster updateClusterStatus(
            @PathParam("id") String id,
            ConnectorClusterStatus status);

    /**
     * Retrieve the connector deployment configurations that need to be placed on this cluster.
     *
     * @param  id              the id of the cluster
     * @param  resourceVersion the resource version to start from
     * @return                 a list of {@link Connector}
     */
    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<ConnectorDeployment> getConnectors(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion);

    /**
     * Updates the status of a connector.
     *
     * @param id     the id of the cluster
     * @param cid    the id of the connector
     * @param status the status of the connector
     */
    @POST
    @Path("/{id}/connectors/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void updateConnectorStatus(
            @PathParam("id") String id,
            @PathParam("cid") String cid,
            ConnectorDeployment.Status status);
}
