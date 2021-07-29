package org.bf2.cos.fleetshard.sync.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.sync.connector.ConnectorDeploymentQueue;

@ApplicationScoped
@Path("/test/connectors")
public class SyncResource {
    @Inject
    ConnectorDeploymentQueue queue;

    @Path("/deployment/provisioner/queue")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public int trigger(Long gv) {
        if (gv == 0) {
            queue.submitPoisonPill();
        } else {
            queue.submit(gv);
        }

        return queue.size();
    }
}
