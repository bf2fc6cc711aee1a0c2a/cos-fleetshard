package org.bf2.cos.fleetshard.sync.it;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus1;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.support.CollectionUtils;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
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
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceProvisionerSanitizeTest.Profile.class)
public class NamespaceProvisionerSanitizeTest extends SyncTestSupport {
    @Inject
    FleetShardClient client;
    @Inject
    FleetShardSyncConfig config;

    @Test
    void namespaceIsProvisioned() {
        final String deploymentId1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
        final String deploymentId2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);
        final String deploymentName1 = ConfigProvider.getConfig().getValue("test.deployment.name.1", String.class);
        final String deploymentName2 = ConfigProvider.getConfig().getValue("test.deployment.name.2", String.class);
        final String deploymentOwner1 = ConfigProvider.getConfig().getValue("test.deployment.owner.1", String.class);
        final String deploymentOwner2 = ConfigProvider.getConfig().getValue("test.deployment.owner.2", String.class);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        Namespace ns1 = until(
            () -> fleetShardClient.getNamespace(deploymentId1),
            Objects::nonNull);

        assertThat(ns1).satisfies(item -> {
            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, KubernetesResourceUtil.sanitizeName(deploymentName1))
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, "foo-acme-com")
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_ID, KubernetesResourceUtil.sanitizeName(deploymentOwner1))
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_ID, "owner-acme-com");
        });

        Namespace ns2 = until(
            () -> fleetShardClient.getNamespace(deploymentId2),
            Objects::nonNull);

        assertThat(ns2).satisfies(item -> {
            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, KubernetesResourceUtil.sanitizeName(deploymentName2))
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, "a--eval")
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_ID, KubernetesResourceUtil.sanitizeName(deploymentOwner2))
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_ID, "a--owner");
        });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return CollectionUtils.mapOf(
                "test.deployment.id.1", uid(),
                "test.deployment.id.2", uid(),
                "test.deployment.name.1", "foo@acme.com",
                "test.deployment.name.2", "--eval",
                "test.deployment.owner.1", "owner@acme.com",
                "test.deployment.owner.2", "--owner",
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(OidcTestResource.class),
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected void configure(WireMockServer server) {
            final String deploymentId1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deploymentId2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);
            final String deploymentName1 = ConfigProvider.getConfig().getValue("test.deployment.name.1", String.class);
            final String deploymentName2 = ConfigProvider.getConfig().getValue("test.deployment.name.2", String.class);
            final String deploymentOwner1 = ConfigProvider.getConfig().getValue("test.deployment.owner.1", String.class);
            final String deploymentOwner2 = ConfigProvider.getConfig().getValue("test.deployment.owner.2", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deploymentId1, deploymentName1, n -> {
                            ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                .id(deploymentOwner1)
                                .kind(ConnectorNamespaceTenantKind.USER);

                            n.setStatus(new ConnectorNamespaceStatus1().state(ConnectorNamespaceState.READY));
                            n.setTenant(tenant);
                            n.setExpiration(new Date().toString());
                        }),
                        namespace(deploymentId2, deploymentName2, n -> {
                            ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                .id(deploymentOwner2)
                                .kind(ConnectorNamespaceTenantKind.USER);

                            n.setStatus(new ConnectorNamespaceStatus1().state(ConnectorNamespaceState.READY));
                            n.setTenant(tenant);
                            n.setExpiration(new Date().toString());
                        }));

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
