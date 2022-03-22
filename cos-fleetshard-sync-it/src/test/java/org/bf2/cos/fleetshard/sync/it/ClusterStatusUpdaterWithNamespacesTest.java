package org.bf2.cos.fleetshard.sync.it;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

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
    @WireMockTestInstance
    com.github.tomakehurst.wiremock.WireMockServer server;

    @Test
    void statusIsUpdated() {
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id() + "/status";
        final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
        final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/connectors/deployment/provisioner/queue");

        until(
            () -> fleetShardClient.getNamespace(deployment1),
            Objects::nonNull);

        until(
            () -> fleetShardClient.getNamespace(deployment2),
            Objects::nonNull);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader("Content-Type", equalTo(APPLICATION_JSON))
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
                "cos.operators.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.cluster.status.sync-interval", "1s",
                "cos.connectors.poll-interval", "disabled",
                "cos.connectors.resync-interval", "disabled",
                "cos.connectors.status.resync-interval", "disabled",
                "cos.connectors.watch", "false");
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
        protected Map<String, String> doStart(com.github.tomakehurst.wiremock.WireMockServer server) {
            final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);
            final String clusterId = ConfigProvider.getConfig().getValue("cos.cluster.id", String.class);
            final String clusterUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + clusterId;
            final String deploymentsUrl = clusterUrl + "/deployments";

            {
                MappingBuilder request = WireMock.get(WireMock.urlPathMatching(
                    "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader("Content-Type", APPLICATION_JSON)
                    .withJsonBody(namespaceList(
                        namespace(deployment1, deployment1, ns -> {
                            ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                .id(uid())
                                .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                            ns.setTenant(tenant);
                            ns.setExpiration(new Date().toString());
                        }),
                        namespace(deployment2, deployment2, ns -> {
                            ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                .id(uid())
                                .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                            ns.setTenant(tenant);
                            ns.setExpiration(new Date().toString());
                        })));

                server.stubFor(request.willReturn(response));
            }

            {
                MappingBuilder request = WireMock.get(WireMock.urlPathEqualTo(deploymentsUrl));
                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader("Content-Type", APPLICATION_JSON)
                    .withJsonBody(deploymentList());

                server.stubFor(request.willReturn(response));
            }

            {
                MappingBuilder request = WireMock.put(WireMock.urlPathMatching(
                    "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/status"));

                ResponseDefinitionBuilder response = WireMock.ok();

                server.stubFor(request.willReturn(response));
            }

            return Map.of("control-plane-base-url", server.baseUrl());
        }

        @Override
        public void inject(TestInjector testInjector) {
            injectServerInstance(testInjector);
        }
    }
}
