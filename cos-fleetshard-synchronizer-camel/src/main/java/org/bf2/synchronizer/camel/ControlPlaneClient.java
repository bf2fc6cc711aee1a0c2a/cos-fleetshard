package org.bf2.synchronizer.camel;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.camel.CamelConnectorStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
@RegisterRestClient(configKey = "kafka-connector-clusters")
public interface ControlPlaneClient {
    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateStatus(@PathParam("id") String id, CamelConnectorStatus status);

    @GET
    @Path("/{id}/connectors/camel")
    @Produces(MediaType.APPLICATION_JSON)
    List<CamelConnector> getConnectors(@PathParam("id") String id);
}
