//usr/bin/env jbang "$0" "$@" ; exit $?
//
//DEPS io.quarkus:quarkus-bom:1.12.2.Final@pom
//DEPS io.quarkus:quarkus-resteasy-jackson
//DEPS io.quarkus:quarkus-kubernetes-client
//DEPS org.bf2:cos-fleetshard-api:1.0.0-SNAPSHOT
//DEPS org.bf2:cos-fleetshard-common:1.0.0-SNAPSHOT
//
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.http.port=9090
//Q:CONFIG quarkus.log.console.format=%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n
//

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.Agent;
import org.bf2.cos.fleetshard.api.AgentStatus;
import org.bf2.cos.fleetshard.api.ConnectorDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Path("/api/managed-services-api/v1/kafka-connector-clusters")
public class cp {
    private static final Logger LOGGER = LoggerFactory.getLogger(cp.class);


    private final Agent cluster;
    private final AtomicLong counter;
    private final Map<String, ConnectorDeployment> connectors;

    public cp() {
        this.cluster = new Agent();
        this.counter = new AtomicLong();
        this.connectors = new HashMap<>();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Agent getAgent(
            @PathParam("id") String id) {

        return cluster;
    }

    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateAgent(
            @PathParam("id") String id,
            AgentStatus status) {

        this.cluster.setStatus(status);
    }

    @GET
    @Path("/{id}/connectors/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConnectorDeployment> getConnectors(
            @PathParam("id") String id,
            @QueryParam("gt_version") long resourceVersion) {

        return connectors.values().stream()
                .filter(c -> resourceVersion < c.getSpec().getResourceVersion())
                .sorted(Comparator.comparingLong(c -> c.getSpec().getResourceVersion()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/connectors/{cid}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateConnector(
            @PathParam("id") String id,
            @PathParam("cid") String cid,
            ConnectorDeployment.Status status) {

        LOGGER.info("Updating status {}", status);
    }

    @POST
    @Path("/{id}/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateConnector(
            ConnectorDeployment connector)
            throws Exception {

        connector.getSpec().setResourceVersion(counter.incrementAndGet());
        connectors.put(connector.getId(), connector);
    }

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(args);
    }
}
