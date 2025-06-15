package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ProcessorDesiredState;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_SERVICE_ACCOUNT;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

@QuarkusTest
@TestProfile(ProcessorProvisionerTest.Profile.class)
public class ProcessorProvisionerTest extends SyncTestSupport {

    public static final String DEPLOYMENT_ID = uid();
    public static final String KAFKA_URL = "kafka.acme.com:2181";
    public static final String KAFKA_CLIENT_ID = uid();
    public static final String KAFKA_CLIENT_SECRET = toBase64(uid());

    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;

    @Test
    void processorIsProvisioned() {
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
                .post("/test/provisioner/processors");

            Secret s1 = until(
                () -> fleetShardClient.getProcessorSecret(clusterId, DEPLOYMENT_ID),
                item -> Objects.equals(
                    "1",
                    item.getMetadata().getLabels().get(Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION)));

            ManagedProcessor mp = until(
                () -> fleetShardClient.getProcessor(clusterId, DEPLOYMENT_ID),
                item -> {
                    return item.getSpec().getDeploymentResourceVersion() == 1L
                        && item.getSpec().getSecret() != null;
                });

            assertThat(s1).satisfies(item -> {
                assertThat(item.getMetadata().getName())
                    .isEqualTo(Secrets.generateProcessorSecretId(mp.getSpec().getDeploymentId()));

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_SERVICE_ACCOUNT))
                    .isObject()
                    .containsEntry("client_id", KAFKA_CLIENT_ID)
                    .containsEntry("client_secret", KAFKA_CLIENT_SECRET);
            });

            assertThat(mp.getMetadata().getName()).startsWith(Resources.PROCESSOR_PREFIX);
            assertThat(mp.getSpec().getKafka().getUrl()).isEqualTo(KAFKA_URL);
            assertThat(mp.getSpec().getSecret()).isEqualTo(s1.getMetadata().getName());
        }
        {
            //
            // Deployment v2
            //

            given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .body(1L)
                .post("/test/provisioner/processors");

            Secret s1 = until(
                () -> fleetShardClient.getProcessorSecret(clusterId, DEPLOYMENT_ID),
                item -> Objects.equals(
                    "2",
                    item.getMetadata().getLabels().get(Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION)));

            ManagedProcessor mc = until(
                () -> fleetShardClient.getProcessor(clusterId, DEPLOYMENT_ID),
                item -> {
                    return item.getSpec().getDeploymentResourceVersion() == 2L
                        && item.getSpec().getSecret() != null;
                });

            assertThat(s1).satisfies(item -> {
                assertThat(item.getMetadata().getName())
                    .isEqualTo(Secrets.generateProcessorSecretId(mc.getSpec().getDeploymentId()));

                assertThatJson(Secrets.extract(item, SECRET_ENTRY_SERVICE_ACCOUNT))
                    .isObject()
                    .containsEntry("client_id", KAFKA_CLIENT_ID)
                    .containsEntry("client_secret", KAFKA_CLIENT_SECRET);
            });

            assertThat(mc.getMetadata().getName()).startsWith(Resources.PROCESSOR_PREFIX);
            assertThat(mc.getSpec().getKafka().getUrl()).isEqualTo(KAFKA_URL);
            assertThat(mc.getSpec().getSecret()).isEqualTo(s1.getMetadata().getName());
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
                "cos.resources.update-interval", "disabled",
                "cos.metrics.recorder.tags.annotations[0]", "my.cos.bf2.org/processor-group",
                "cos.metrics.recorder.tags.labels[0]", "cos.bf2.org/organization-id",
                "cos.metrics.recorder.tags.labels[1]", "cos.bf2.org/pricing-tier");
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
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                req -> req.withQueryParam("gt_version", equalTo("0")),
                resp -> {
                    JsonNode body = processorDeploymentList(
                        processorDeployment(DEPLOYMENT_ID, 1L,
                            depl -> {
                                depl.getMetadata().annotations(Map.of());
                            },
                            spec -> {
                                spec.namespaceId(clusterId);
                                spec.processorId("processor-1");
                                spec.processorTypeId("processor-type-1");
                                spec.processorResourceVersion(1L);
                                spec.kafka(
                                    new KafkaConnectionSettings()
                                        .url(KAFKA_URL));
                                spec.serviceAccount(
                                    new ServiceAccount()
                                        .clientId(KAFKA_CLIENT_ID)
                                        .clientSecret(KAFKA_CLIENT_SECRET));
                                spec.shardMetadata(node(n -> {
                                    n.withArray("operators").addObject()
                                        .put("type", "camel-connector-operator")
                                        .put("version", "[1.0.0,2.0.0)");
                                }));

                                spec.desiredState(ProcessorDesiredState.READY);
                            }));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                req -> req.withQueryParam("gt_version", equalTo("1")),
                resp -> {
                    JsonNode body = processorDeploymentList(
                        processorDeployment(DEPLOYMENT_ID, 2L,
                            depl -> {
                                depl.getMetadata().annotations(Map.of());
                            },
                            spec -> {
                                spec.namespaceId(clusterId);
                                spec.processorId("processor-1");
                                spec.processorTypeId("processor-type-1");
                                spec.processorResourceVersion(1L);
                                spec.kafka(
                                    new KafkaConnectionSettings()
                                        .url(KAFKA_URL));
                                spec.serviceAccount(
                                    new ServiceAccount()
                                        .clientId(KAFKA_CLIENT_ID)
                                        .clientSecret(KAFKA_CLIENT_SECRET));
                                spec.shardMetadata(node(n -> {
                                    n.withArray("operators").addObject()
                                        .put("type", "camel-connector-operator")
                                        .put("version", "[1.0.0,2.0.0)");
                                }));
                                spec.desiredState(ProcessorDesiredState.READY);
                            }));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });
        }
    }
}
