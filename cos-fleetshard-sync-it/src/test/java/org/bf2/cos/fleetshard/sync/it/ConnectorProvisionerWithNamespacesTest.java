package org.bf2.cos.fleetshard.sync.it;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorDesiredState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

@QuarkusTest
@TestProfile(ConnectorProvisionerWithNamespacesTest.Profile.class)
public class ConnectorProvisionerWithNamespacesTest extends SyncTestSupport {
    public static final String KAFKA_URL = "kafka.acme.com:2181";
    public static final String KAFKA_CLIENT_ID = uid();
    public static final String KAFKA_CLIENT_SECRET = toBase64(uid());

    @Test
    void connectorIsProvisionedInNamespace() {
        final String deploymentId = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        Namespace ns1 = until(
            () -> fleetShardClient.getNamespace(deploymentId),
            item -> {
                return item != null && item.getStatus() != null && "Active".equals(item.getStatus().getPhase());
            });

        assertThat(ns1).satisfies(item -> {
            assertThat(item.getMetadata().getName())
                .isEqualTo(Namespaces.generateNamespaceId(deploymentId));

            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_CLUSTER_ID, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_NAMESPACE_ID, deploymentId)
                .containsEntry(Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_CREATED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_PART_OF, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE)
                .containsEntry(Resources.LABEL_KUBERNETES_INSTANCE, deploymentId);
        });

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/connectors");

        until(
            () -> fleetShardClient.getConnector(deploymentId, deploymentId),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getSpec().getDeployment().getSecret() != null;
            });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.deployment.id", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.operators.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.cluster.status.sync-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.connectors.status.resync-interval", "disabled");
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
        protected Map<String, String> doStart(WireMockServer server) {
            final String deploymentId = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);
            final String clusterId = ConfigProvider.getConfig().getValue("cos.cluster.id", String.class);
            final String clusterUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + clusterId;
            final String deploymentsUrl = clusterUrl + "/deployments";
            final String statusUrl = clusterUrl + "/deployments/.*/status";

            {
                //
                // Namespaces
                //

                MappingBuilder request = WireMock.get(WireMock.urlPathMatching(
                    "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader("Content-Type", APPLICATION_JSON)
                    .withJsonBody(namespaceList(
                        namespace(deploymentId, deploymentId, ns -> {
                            ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                .id(uid())
                                .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                            ns.setTenant(tenant);
                            ns.setExpiration(new Date().toString());
                        })));

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Deployments
                //

                JsonNode list = deploymentList(
                    deployment(deploymentId, 1L, spec -> {
                        spec.namespaceId(deploymentId);
                        spec.connectorId(deploymentId);
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

                {
                    MappingBuilder request = WireMock.get(WireMock.urlPathEqualTo(deploymentsUrl))
                        .withQueryParam("gt_version", equalTo("0"));
                    ResponseDefinitionBuilder response = WireMock.aResponse()
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withJsonBody(list);

                    server.stubFor(request.willReturn(response));

                }
            }

            {
                //
                // Status
                //

                MappingBuilder request = WireMock.put(WireMock.urlPathMatching(statusUrl));
                ResponseDefinitionBuilder response = WireMock.ok();

                server.stubFor(request.willReturn(response));
            }

            return Map.of("control-plane-base-url", server.baseUrl());
        }
    }
}
