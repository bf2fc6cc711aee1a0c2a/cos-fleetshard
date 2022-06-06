package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(AuthUrlStaticTest.Profile.class)
public class AuthUrlStaticTest extends SyncTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void namespaceIsProvisioned() {
        UrlPathPattern ssoUrl = urlPathMatching("/api/kafkas_mgmt/v1/sso_providers");
        UrlPathPattern cosUrl = urlPathMatching("/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces");

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            server.verify(exactly(1), getRequestedFor(cosUrl));
            server.verify(exactly(0), getRequestedFor(ssoUrl));
        });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "disabled",
                "cos.manager.sso-uri", "${mas-sso-base-url}/auth/realms/${mas-sso-realm}");
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
            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList());
                });
        }
    }
}
