package org.bf2.cos.fleetshard.operator.sync;

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

import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
@RegisterRestClient(configKey = "cos-control-plane")
public interface ConnectorsControlPlane {
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
    void updateConnectorCluster(
            @PathParam("id") String id,
            ConnectorClusterStatus status);

    /**
     * Retrieve the connector cluster configuration.
     *
     * @param  id the id of the cluster
     * @return    a list of {@link ConnectorCluster}
     */
    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorCluster getConnectorCluster(@PathParam("id") String id);

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
    List<Connector<?, ?>> getConnectors(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion);

    /**
     * Subscribe to the connector deployment configurations that need to be placed on this cluster.
     *
     * @param  id              the id of the cluster
     * @param  resourceVersion the resource version to start from
     * @param  watch           if we want to watch or not
     * @return                 a list of {@link Connector}
     */
    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    Publisher<Connector<?, ?>> getConnectors(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion,
            @QueryParam("watch") boolean watch);

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
    void updateConnector(@PathParam("id") String id, @PathParam("cid") String cid, ConnectorStatus status);
}
