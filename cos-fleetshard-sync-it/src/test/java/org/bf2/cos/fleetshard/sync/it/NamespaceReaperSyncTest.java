package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceReaperSyncTest.Profile.class)
public class NamespaceReaperSyncTest extends NamespaceReaperSyncTestBase {

    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void namespaceIsProvisioned() {
        final String ns1 = ConfigProvider.getConfig().getValue("test.ns.id.1", String.class);
        final String ns2 = ConfigProvider.getConfig().getValue("test.ns.id.2", String.class);
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id() + "/status";

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        until(
            () -> fleetShardClient.getNamespace(ns1),
            Objects::nonNull);
        until(
            () -> fleetShardClient.getNamespace(ns2),
            Objects::nonNull);

        server.until(
            putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(jp("$.namespaces.size()", "2"))
                .withRequestBody(jp("$.namespaces[0].phase", Namespaces.PHASE_READY))
                .withRequestBody(jp("$.namespaces[1].phase", Namespaces.PHASE_READY)));

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(fleetShardClient.getNamespace(ns1)).isPresent();
        });
        untilAsserted(() -> {
            assertThat(fleetShardClient.getNamespace(ns2)).isNotPresent();
        });

        server.until(
            putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(jp("$.namespaces.size()", "1"))
                .withRequestBody(jp("$.namespaces[0].phase", Namespaces.PHASE_READY)));
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.ns.id.1", uid(),
                "test.ns.id.2", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "1s",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "1s");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }
}
