package org.bf2.synchronizer;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.AddonParameter;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.cluster.ConnectorClusterSpec;
import org.bf2.cos.fleetshard.api.cluster.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnectorSpec;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
@RegisterRestClient(configKey = "kafka-connector-clusters")
public interface ConnectorClusterClient {

    /**
     * Register a new {@link ConnectorClusterSpec} instance to the control plane.
     * </p>
     * The result of this call is an immutable {@link ConnectorClusterSpec}.
     *
     * @param  metadata optional metadata
     * @return          an instance of {@link ConnectorClusterSpec}
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ConnectorClusterSpec registerAgent(Map<String, String> metadata);

    /**
     * Retrieve the parameter the agent uses to securely establish the communication with the control plane.
     *
     * @param  id the id of the cluster
     * @return    a list of {@link AddonParameter}
     */
    @GET
    @Path("/{id}/addon-parameters")
    @Consumes(MediaType.APPLICATION_JSON)
    List<AddonParameter> getAgentParameters(@PathParam("id") String id);

    /**
     * Updates the status of the cluster.
     *
     * @param id     the id of the cluster
     * @param status the status of the cluster
     */
    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateAgent(@PathParam("id") String id, ConnectorClusterStatus status);


    /**
     * Retrieve the connectors that neexd to be placed on this cluster.
     *
     * @param  id the id of the cluster
     * @return    a list of {@link CamelConnectorSpec}
     */
    @GET
    @Path("/{id}/connectors/camel")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Connector> getConnectors(@PathParam("id") String id);

    /**
     * Updates the status of a connector.
     *
     * @param id     the id of the cluster
     * @param cid    the id of the connector
     * @param status the status of the connector
     */
    @POST
    @Path("/{id}/connectors/{cid}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateConnector(@PathParam("id") String id, @PathParam("cid") String cid, ConnectorStatus status);
}
