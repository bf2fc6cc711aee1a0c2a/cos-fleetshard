package org.bf2.cos.fleetshard.sync.it;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorDesiredState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

@QuarkusTest
@TestProfile(PaginationTest.Profile.class)
public class PaginationTest extends SyncTestSupport {
    public static final String KAFKA_URL = "kafka.acme.com:2181";
    public static final String KAFKA_CLIENT_ID = uid();
    public static final String KAFKA_CLIENT_SECRET = toBase64(uid());

    @WireMockTestInstance
    WireMockServer server;

    @Test
    void namespaceIsProvisioned() {
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
        until(
            () -> fleetShardClient.getConnector(deployment1, deployment1),
            Objects::nonNull);
        until(
            () -> fleetShardClient.getConnector(deployment2, deployment2),
            Objects::nonNull);

        final String namespacesUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces";
        server.verify(getRequestedFor(urlPathMatching(namespacesUrl)).withQueryParam("page", equalTo("1")));
        server.verify(getRequestedFor(urlPathMatching(namespacesUrl)).withQueryParam("page", equalTo("2")));

        final String deploymentsUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments";
        server.verify(getRequestedFor(urlPathMatching(deploymentsUrl)).withQueryParam("page", equalTo("1")));
        server.verify(getRequestedFor(urlPathMatching(deploymentsUrl)).withQueryParam("page", equalTo("2")));
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.deployment.id.1", uid(),
                "test.deployment.id.2", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", getId(),
                "cos.operators.namespace", getId(),
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
            final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);
            final String clusterId = ConfigProvider.getConfig().getValue("cos.cluster.id", String.class);
            final String clusterUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + clusterId;
            final String deploymentsUrl = clusterUrl + "/deployments";
            final String statusUrl = clusterUrl + "/deployments/.*/status";

            {
                //
                // Namespaces Page 1
                //

                ObjectNode list = namespaceList(
                    namespace(deployment1, deployment1, ns -> {
                        ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                            .id(uid())
                            .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                        ns.setTenant(tenant);
                        ns.setExpiration(new Date().toString());
                    }));

                list.put("total", "2");

                MappingBuilder request = WireMock.get(WireMock.urlPathMatching(
                    "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces"))
                    .withQueryParam("page", equalTo("1"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                    .withJsonBody(list);

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Namespaces Page 2
                //

                ObjectNode list = namespaceList(
                    namespace(deployment2, deployment2, ns -> {
                        ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                            .id(uid())
                            .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                        ns.setTenant(tenant);
                        ns.setExpiration(new Date().toString());
                    }));

                list.put("total", "2");

                MappingBuilder request = WireMock.get(WireMock.urlPathMatching(
                    "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces"))
                    .withQueryParam("page", equalTo("2"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                    .withJsonBody(list);

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Deployments Page 1
                //

                ObjectNode list = deploymentList(
                    deployment(deployment1, 1L, spec -> {
                        spec.namespaceId(deployment1);
                        spec.connectorId(deployment1);
                        spec.connectorTypeId("connector-type-1");
                        spec.connectorResourceVersion(1L);
                        spec.kafka(
                            new KafkaConnectionSettings()
                                .url(KAFKA_URL));
                        spec.serviceAccount(
                            new ServiceAccount()
                                .clientId(KAFKA_CLIENT_ID)
                                .clientSecret(KAFKA_CLIENT_SECRET));
                        spec.connectorSpec(node(n -> {
                            n.with("connector").put("foo", "connector-bar");
                            n.with("kafka").put("topic", "kafka-bar");
                        }));
                        spec.shardMetadata(node(n -> {
                            n.put("connector_type", "sink");
                            n.put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.1.0");
                            n.withArray("operators").addObject()
                                .put("type", "camel-connector-operator")
                                .put("version", "[1.0.0,2.0.0)");
                        }));
                        spec.desiredState(ConnectorDesiredState.READY);
                    }));

                list.put("total", "2");

                MappingBuilder request = WireMock.get(WireMock.urlPathEqualTo(deploymentsUrl))
                    .withQueryParam("page", equalTo("1"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                    .withJsonBody(list);

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Deployments Page 2
                //

                ObjectNode list = deploymentList(
                    deployment(deployment2, 2L, spec -> {
                        spec.namespaceId(deployment2);
                        spec.connectorId(deployment2);
                        spec.connectorTypeId("connector-type-1");
                        spec.connectorResourceVersion(1L);
                        spec.kafka(
                            new KafkaConnectionSettings()
                                .url(KAFKA_URL));
                        spec.serviceAccount(
                            new ServiceAccount()
                                .clientId(KAFKA_CLIENT_ID)
                                .clientSecret(KAFKA_CLIENT_SECRET));
                        spec.connectorSpec(node(n -> {
                            n.with("connector").put("foo", "connector-bar");
                            n.with("kafka").put("topic", "kafka-bar");
                        }));
                        spec.shardMetadata(node(n -> {
                            n.put("connector_type", "sink");
                            n.put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.1.0");
                            n.withArray("operators").addObject()
                                .put("type", "camel-connector-operator")
                                .put("version", "[1.0.0,2.0.0)");
                        }));
                        spec.desiredState(ConnectorDesiredState.READY);
                    }));

                list.put("total", "2");

                MappingBuilder request = WireMock.get(WireMock.urlPathEqualTo(deploymentsUrl))
                    .withQueryParam("page", equalTo("2"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                    .withJsonBody(list);

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Status
                //

                MappingBuilder request = WireMock.put(WireMock.urlPathMatching(statusUrl));
                ResponseDefinitionBuilder response = WireMock.ok();

                server.stubFor(request.willReturn(response));
            }
        }
    }
}
