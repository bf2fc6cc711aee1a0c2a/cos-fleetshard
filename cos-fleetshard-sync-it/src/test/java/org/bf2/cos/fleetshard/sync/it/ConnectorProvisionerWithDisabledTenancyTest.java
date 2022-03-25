package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorDesiredState;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
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
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

@QuarkusTest
@TestProfile(ConnectorProvisionerWithDisabledTenancyTest.Profile.class)
public class ConnectorProvisionerWithDisabledTenancyTest extends SyncTestSupport {
    public static final String KAFKA_URL = "kafka.acme.com:2181";
    public static final String KAFKA_CLIENT_ID = uid();
    public static final String KAFKA_CLIENT_SECRET = toBase64(uid());

    @WireMockTestInstance
    WireMockServer server;

    @ConfigProperty(name = "test.namespace")
    String ns;
    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;

    @Test
    void connectorIsProvisioned() {
        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/all");

        until(
            () -> fleetShardClient.getSecret(ns, clusterId),
            item -> Objects.equals(
                "1",
                item.getMetadata().getLabels().get(Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION)));

        until(
            () -> fleetShardClient.getConnector(ns, clusterId),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getSpec().getDeployment().getSecret() != null;
            });

        final String namespacesUrl = "/api/connector_mgmt/v1/kafka_connector_clusters/.*/namespaces";
        final RequestPatternBuilder namespacesReq = anyRequestedFor(urlPathMatching(namespacesUrl));

        server.verify(0, namespacesReq);
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.operators.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.tenancy.enabled", "false",
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
                "/api/connector_mgmt/v1/kafka_connector_clusters/.*/namespaces",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.POST,
                "/api/connector_mgmt/v1/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/kafka_connector_clusters/.*/deployments",
                resp -> {
                    JsonNode body = deploymentList(
                        deployment(clusterId, 1L, spec -> {
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
        }
    }
}
