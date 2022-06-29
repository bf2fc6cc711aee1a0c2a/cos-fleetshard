package org.bf2.cos.fleetshard.sync.it;

import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespaceReaperDelTestBase extends NamespaceReaperTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void namespaceIsProvisioned() {
        final String deploymentId = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + clientConfig.cluster().id()
            + "/status";

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        until(
            () -> fleetShardClient.getNamespace(deploymentId),
            Objects::nonNull);

        server.until(
            putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(jp("$.namespaces.size()", "1"))
                .withRequestBody(jp("$.namespaces[0].phase", Namespaces.PHASE_READY))
                .withRequestBody(jp("$.namespaces[0].version", "0")));

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(fleetShardClient.getNamespace(deploymentId)).isNotPresent();
        });

        server.until(
            putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$.namespaces", WireMock.absent())));
    }
}
