package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.ANNOTATION_NAMESPACE_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ReSyncTest.Profile.class)
public class ReSyncTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();
    public static final String CONNECTOR_ID = uid();

    @WireMockTestInstance
    WireMockServer server;

    @ConfigProperty(name = "test.namespace")
    String ns;

    @Test
    void namespaceIsProvisioned() {
        final String namespacesUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces";
        final String deploymentsUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments";

        kubernetesClient.resources(Namespace.class).createOrReplace(new NamespaceBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Namespaces.generateNamespaceId(DEPLOYMENT_ID))
                .addToLabels(LABEL_CLUSTER_ID, config.cluster().id())
                .addToAnnotations(ANNOTATION_NAMESPACE_RESOURCE_VERSION, "20")
                .build())
            .build());

        kubernetesClient.resources(ManagedConnector.class).inNamespace(ns).createOrReplace(new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Connectors.generateConnectorId(DEPLOYMENT_ID))
                .addToLabels(LABEL_CLUSTER_ID, config.cluster().id())
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withDeployment(new DeploymentSpecBuilder().withDeploymentResourceVersion(10L).build())
                .withClusterId(config.cluster().id())
                .withConnectorId(CONNECTOR_ID)
                .withDeploymentId(DEPLOYMENT_ID)
                .withOperatorSelector(new OperatorSelectorBuilder().withId(uid()).build())
                .build())
            .build());

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .post("/test/provisioner/sync");

        untilAsserted(() -> {
            server.verify(1, getRequestedFor(urlPathMatching(namespacesUrl)).withQueryParam("gt_version", equalTo("0")));
            server.verify(1, getRequestedFor(urlPathMatching(deploymentsUrl)).withQueryParam("gt_version", equalTo("0")));
        });

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .post("/test/provisioner/sync");

        untilAsserted(() -> {
            server.verify(getRequestedFor(urlPathMatching(namespacesUrl)).withQueryParam("gt_version", equalTo("20")));
            server.verify(getRequestedFor(urlPathMatching(deploymentsUrl)).withQueryParam("gt_version", equalTo("10")));
        });

        untilAsserted(() -> {
            RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .post("/test/provisioner/sync");

            server.verify(2, getRequestedFor(urlPathMatching(namespacesUrl)).withQueryParam("gt_version", equalTo("0")));
            server.verify(2, getRequestedFor(urlPathMatching(deploymentsUrl)).withQueryParam("gt_version", equalTo("0")));
        });

    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.operators.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.cluster.status.sync-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "5s",
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
                    .withJsonBody(namespaceList());

                server.stubFor(request.willReturn(response));
            }

            {
                //
                // Deployments
                //

                MappingBuilder request = WireMock.get(WireMock.urlPathEqualTo(deploymentsUrl));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader("Content-Type", APPLICATION_JSON)
                    .withJsonBody(deploymentList());

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

            return Map.of("control-plane-base-url", server.baseUrl());
        }

        @Override
        public void inject(TestInjector testInjector) {
            injectServerInstance(testInjector);
        }
    }
}
