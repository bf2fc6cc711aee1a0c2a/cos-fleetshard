package org.bf2.cos.fleetshard.operator.sync;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
@RegisterRestClient(configKey = "kafka-connector-clusters")
public interface ConnectorClusterClient {
    /**
     * Updates the status of the cluster.
     *
     * @param id the id of the cluster
     * @param status the status of the cluster
     */
    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateAgent(@PathParam("id") String id, String status);

    /**
     * Retrieve the connector deployment configurations that need to be placed on this cluster.
     *
     * @param id the id of the cluster
     * @return a list of {@link Connector}
     */
    @GET
    @Path("/{id}/connectors/")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Connector> getConnectors(@PathParam("id") String id);

    /**
     * Updates the status of a connector.
     *
     * @param id the id of the cluster
     * @param cid the id of the connector
     * @param status the status of the connector
     */
    @POST
    @Path("/{id}/connectors/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateConnector(@PathParam("id") String id, @PathParam("cid") String cid, ConnectorStatus status);
}
