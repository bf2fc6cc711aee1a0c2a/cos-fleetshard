package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorDesiredState;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_META;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_SERVICE_ACCOUNT;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

@QuarkusTest
@TestProfile(ConnectorProvisionerTest.Profile.class)
public class ConnectorProvisionerTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();
    public static final String KAFKA_URL = "kafka.acme.com:2181";
    public static final String KAFKA_CLIENT_ID = uid();
    public static final String KAFKA_CLIENT_SECRET = toBase64(uid());

    @WireMockTestInstance
    WireMockServer server;

    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;

    @Test
    void connectorIsProvisioned() {
        {
            given()
                .contentType(MediaType.TEXT_PLAIN)
                .body(0L)
                .post("/test/provisioner/namespaces");

            Namespace ns1 = until(
                () -> fleetShardClient.getNamespace(clusterId),
                Objects::nonNull);
        }

        {
            //
            // Deployment v1
            //

            given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .body(0L)
                .post("/test/provisioner/connectors");

            Secret s1 = until(
                () -> fleetShardClient.getSecret(clusterId, DEPLOYMENT_ID),
                item -> Objects.equals(
                    "1",
                    item.getMetadata().getLabels().get(Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION)));

            ManagedConnector mc = until(
                () -> fleetShardClient.getConnector(clusterId, DEPLOYMENT_ID),
                item -> {
                    return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                        && item.getSpec().getDeployment().getSecret() != null;
                });

            assertThat(s1).satisfies(item -> {
                assertThat(item.getMetadata().getName())
                    .isEqualTo(Secrets.generateConnectorSecretId(mc.getSpec().getDeploymentId()));

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_SERVICE_ACCOUNT))
                    .isObject()
                    .containsEntry("client_id", KAFKA_CLIENT_ID)
                    .containsEntry("client_secret", KAFKA_CLIENT_SECRET);

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_CONNECTOR))
                    .inPath("connector")
                    .isObject()
                    .containsEntry("foo", "connector-foo");
                assertThatJson(Secrets.extract(item, SECRET_ENTRY_CONNECTOR))
                    .inPath("kafka")
                    .isObject()
                    .containsEntry("topic", "kafka-foo");

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_META))
                    .isObject()
                    .containsEntry("connector_type", "sink");
                assertThatJson(Secrets.extract(item, SECRET_ENTRY_META))
                    .isObject()
                    .containsEntry("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.0.1");
            });

            assertThat(mc.getMetadata().getName()).startsWith(Resources.CONNECTOR_PREFIX);
            assertThat(mc.getSpec().getDeployment().getKafka().getUrl()).isEqualTo(KAFKA_URL);
            assertThat(mc.getSpec().getDeployment().getSecret()).isEqualTo(s1.getMetadata().getName());
        }
        {
            //
            // Deployment v2
            //

            given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .body(1L)
                .post("/test/provisioner/connectors");

            Secret s1 = until(
                () -> fleetShardClient.getSecret(clusterId, DEPLOYMENT_ID),
                item -> Objects.equals(
                    "2",
                    item.getMetadata().getLabels().get(Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION)));

            ManagedConnector mc = until(
                () -> fleetShardClient.getConnector(clusterId, DEPLOYMENT_ID),
                item -> {
                    return item.getSpec().getDeployment().getDeploymentResourceVersion() == 2L
                        && item.getSpec().getDeployment().getSecret() != null;
                });

            assertThat(s1).satisfies(item -> {
                assertThat(item.getMetadata().getName())
                    .isEqualTo(Secrets.generateConnectorSecretId(mc.getSpec().getDeploymentId()));

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_SERVICE_ACCOUNT))
                    .isObject()
                    .containsEntry("client_id", KAFKA_CLIENT_ID)
                    .containsEntry("client_secret", KAFKA_CLIENT_SECRET);

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_CONNECTOR))
                    .inPath("connector")
                    .isObject()
                    .containsEntry("foo", "connector-bar");
                assertThatJson(Secrets.extract(item, SECRET_ENTRY_CONNECTOR))
                    .inPath("kafka")
                    .isObject()
                    .containsEntry("topic", "kafka-bar");

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_META))
                    .isObject()
                    .containsEntry("connector_type", "sink");
                assertThatJson(Secrets.extract(item, SECRET_ENTRY_META))
                    .isObject()
                    .containsEntry("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.1.0");
            });

            assertThat(mc.getMetadata().getName()).startsWith(Resources.CONNECTOR_PREFIX);
            assertThat(mc.getSpec().getDeployment().getKafka().getUrl()).isEqualTo(KAFKA_URL);
            assertThat(mc.getSpec().getDeployment().getSecret()).isEqualTo(s1.getMetadata().getName());
        }
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.update-interval", "disabled");
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
            final String clusterId = ConfigProvider.getConfig().getValue("cos.cluster.id", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(clusterId, clusterId));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                req -> req.withQueryParam("gt_version", equalTo("0")),
                resp -> {
                    JsonNode body = deploymentList(
                        deployment(DEPLOYMENT_ID, 1L, spec -> {
                            spec.namespaceId(clusterId);
                            spec.connectorId("connector-1");
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
                                n.with("connector").put("foo", "connector-foo");
                                n.with("kafka").put("topic", "kafka-foo");
                            }));
                            spec.shardMetadata(node(n -> {
                                n.put("connector_type", "sink");
                                n.put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.0.1");
                                n.withArray("operators").addObject()
                                    .put("type", "camel-connector-operator")
                                    .put("version", "[1.0.0,2.0.0)");
                            }));
                            spec.desiredState(ConnectorDesiredState.READY);
                        }));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                req -> req.withQueryParam("gt_version", equalTo("1")),
                resp -> {
                    JsonNode body = deploymentList(
                        deployment(DEPLOYMENT_ID, 2L, spec -> {
                            spec.namespaceId(clusterId);
                            spec.connectorId("connector-1");
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

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });
        }
    }
}
