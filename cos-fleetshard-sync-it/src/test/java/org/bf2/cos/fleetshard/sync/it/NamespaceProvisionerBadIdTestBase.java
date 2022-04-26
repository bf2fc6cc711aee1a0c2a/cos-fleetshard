package org.bf2.cos.fleetshard.sync.it;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.Namespace;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespaceProvisionerBadIdTestBase extends SyncTestSupport {
    @Inject
    FleetShardClient client;
    @Inject
    FleetShardSyncConfig config;

    @Test
    void namespaceIsProvisioned() {
        final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
        final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(fleetShardClient.getKubernetesClient().v1().events().inNamespace(config.namespace()).list().getItems())
                .anySatisfy(e -> {
                    assertThat(e.getInvolvedObject().getKind()).isEqualTo(ManagedConnectorCluster.class.getSimpleName());
                    assertThat(e.getType()).isEqualTo("Warning");
                    assertThat(e.getReason()).isEqualTo("FailedToCreateOrUpdateResource");
                    assertThat(e.getMessage()).contains("Unable to create or update namespace " + deployment1);
                });
        });

        Namespace ns2 = until(
            () -> fleetShardClient.getNamespace(deployment2),
            Objects::nonNull);

        assertThat(ns2).satisfies(item -> {
            assertThat(item.getMetadata().getName())
                .isEqualTo(client.generateNamespaceId(deployment2));

            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_CLUSTER_ID, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_NAMESPACE_ID, deployment2)
                .containsEntry(Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_CREATED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_PART_OF, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE)
                .containsEntry(Resources.LABEL_KUBERNETES_INSTANCE, deployment2)
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_KIND, ConnectorNamespaceTenantKind.ORGANISATION.getValue())
                .containsKey(Resources.LABEL_NAMESPACE_TENANT_ID);
        });
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected void configure(WireMockServer server) {
            final String deploymentId1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deploymentId2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);
            final String deploymentName1 = ConfigProvider.getConfig().getValue("test.deployment.name.1", String.class);
            final String deploymentName2 = ConfigProvider.getConfig().getValue("test.deployment.name.2", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deploymentId1, deploymentName1),
                        namespace(deploymentId2, deploymentName2));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                resp -> resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON).withJsonBody(deploymentList()));

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());
        }
    }
}
