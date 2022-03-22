package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ConnectorDeletedTest.Profile.class)
public class ConnectorDeletedTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();
    public static final String CONNECTOR_ID = uid();

    @WireMockTestInstance
    com.github.tomakehurst.wiremock.WireMockServer server;

    @ConfigProperty(name = "test.namespace")
    String ns;

    @Test
    void statusIsUpdated() {
        final String clusterUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id();
        final String statusUrl = clusterUrl + "/deployments/" + DEPLOYMENT_ID + "/status";

        final Condition condition = new Condition(null, uid(), null, uid(), uid(), uid());
        final Operator operator = new Operator(uid(), "operator-type", "1.2.3");

        final ManagedConnector connector = new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Connectors.generateConnectorId(DEPLOYMENT_ID))
                .withNamespace(ns)
                .addToLabels(LABEL_CLUSTER_ID, config.cluster().id())
                .addToLabels(LABEL_CONNECTOR_ID, CONNECTOR_ID)
                .addToLabels(LABEL_DEPLOYMENT_ID, DEPLOYMENT_ID)
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(config.cluster().id())
                .withConnectorId(CONNECTOR_ID)
                .withDeploymentId(DEPLOYMENT_ID)
                .build())
            .build();

        connector.getSpec().setOperatorSelector(new OperatorSelectorBuilder().withId(operator.getId()).build());

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(ns)
            .create(connector);

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_READY)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(ns)
            .withName(connector.getMetadata().getName())
            .replaceStatus(connector);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'ready')]")));
        });

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_DELETED)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(ns)
            .withName(connector.getMetadata().getName())
            .replaceStatus(connector);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'deleted')]")));
        });

        untilAsserted(() -> {
            assertThat(
                kubernetesClient
                    .resources(ManagedConnector.class)
                    .inNamespace(ns)
                    .withName(connector.getMetadata().getName())
                    .get()).isNull();
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
                "cos.connectors.poll-interval", "disabled",
                "cos.connectors.resync-interval", "disabled",
                "cos.connectors.provisioner.queue-timeout", "disabled",
                "cos.connectors.status.resync-interval", "1s");
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
            {
                MappingBuilder request = WireMock.get(
                    urlPathMatching(
                        "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces"));

                ResponseDefinitionBuilder response = WireMock.aResponse()
                    .withHeader("Content-Type", APPLICATION_JSON)
                    .withJsonBody(namespaceList());

                server.stubFor(request.willReturn(response));
            }

            {
                MappingBuilder request = WireMock.put(
                    urlPathMatching("/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status"))
                    .withRequestBody(
                        matchingJsonPath("$[?($.phase == 'ready')])"));

                server.stubFor(request.willReturn(WireMock.ok()));
            }

            {
                MappingBuilder request = WireMock.put(
                    urlPathMatching("/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status"))
                    .withRequestBody(
                        matchingJsonPath("$[?($.phase == 'deleted')])"));

                server.stubFor(request.willReturn(WireMock.status(410)));
            }

            return Map.of("control-plane-base-url", server.baseUrl());
        }

        @Override
        public void inject(TestInjector testInjector) {
            injectServerInstance(testInjector);
        }
    }
}
