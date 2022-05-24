package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ClusterStatusUpdaterWithNamespacesTest.Profile.class)
public class ClusterStatusUpdaterWithNamespacesTest extends SyncTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void statusIsUpdated() {
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id() + "/status";
        final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
        final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .post("/test/provisioner/all");

        until(
            () -> fleetShardClient.getNamespace(deployment1),
            Objects::nonNull);

        until(
            () -> fleetShardClient.getNamespace(deployment2),
            Objects::nonNull);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(jp("$.phase", "ready"))
                .withRequestBody(jp("$.namespaces.size()", "2"))
                .withRequestBody(jp("$.namespaces[?(@.id == '" + deployment1 + "')].phase", Namespaces.PHASE_READY))
                .withRequestBody(jp("$.namespaces[?(@.id == '" + deployment2 + "')].phase", Namespaces.PHASE_READY)));
        });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.deployment.id.1", uid(),
                "test.deployment.id.2", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "1s",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends org.bf2.cos.fleetshard.sync.it.support.ControlPlaneTestResource {
        @Override
        protected void configure(FleetManagerMockServer server) {
            final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deployment1, deployment1),
                        namespace(deployment2, deployment2));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(deploymentList());
                });

            server.stubMatching(
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/status",
                () -> WireMock.ok());
        }
    }
}
