package org.bf2.cos.fleetshard.sync.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.sync.resources.ConnectorDeploymentProvisioner;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.bf2.cos.fleetshard.sync.resources.ResourcePoll;

@ApplicationScoped
@Path("/test")
public class SyncResource {
    @Inject
    ConnectorNamespaceProvisioner namespaceProvisioner;
    @Inject
    ConnectorDeploymentProvisioner deploymentProvisioner;
    @Inject
    ResourcePoll resourceSync;

    @Path("/provisioner/namespaces")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void pollNamespaces(Long gv) {
        namespaceProvisioner.poll(gv);
    }

    @Path("/provisioner/connectors")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void pollConnectors(Long gv) {
        deploymentProvisioner.poll(gv);
    }

    @Path("/provisioner/all")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void poll() {
        namespaceProvisioner.poll(0);
        deploymentProvisioner.poll(0);
    }

    @Path("/provisioner/sync")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sync() {
        resourceSync.run();
    }
}
