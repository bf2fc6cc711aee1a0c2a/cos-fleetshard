package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceReaperWithLeftoversTest.Profile.class)
public class NamespaceReaperWithLeftoversTest extends NamespaceReaperTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void namespaceIsProvisioned() {
        final String deploymentId = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id() + "/status";
        final String namespaceName = fleetShardClient.generateNamespaceId(deploymentId);

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
                .withRequestBody(jp("$.namespaces[0].version", "0"))
                .withRequestBody(jp("$.namespaces[0].connectors_deployed", "0")));

        final ManagedConnector connector = new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Connectors.generateConnectorId(deploymentId))
                .withNamespace(namespaceName)
                .addToLabels(LABEL_CLUSTER_ID, config.cluster().id())
                .addToLabels(LABEL_CONNECTOR_ID, deploymentId)
                .addToLabels(LABEL_DEPLOYMENT_ID, deploymentId)
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(config.cluster().id())
                .withConnectorId(deploymentId)
                .withDeploymentId(deploymentId)
                .withOperatorSelector(new OperatorSelectorBuilder().withId(deploymentId).build())
                .build())
            .build();

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .create(connector);

        untilAsserted(() -> {
            assertThat(fleetShardClient.getAllConnectors()).isNotEmpty();
            assertThat(fleetShardClient.getConnectors(connector.getMetadata().getNamespace())).isNotEmpty();
        });

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        server.until(
            putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$.namespaces", WireMock.absent())));

    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.namespace.delete.state", ConnectorNamespaceState.DELETING.getValue(),
                "test.deployment.id", uid(),
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
