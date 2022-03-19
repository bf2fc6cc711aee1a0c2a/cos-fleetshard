package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.it.resources.OidcTestResource;
import org.bf2.cos.fleetshard.it.resources.WireMockTestInstance;
import org.bf2.cos.fleetshard.it.resources.WireMockTestResource;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.fabric8.kubernetes.api.model.Condition;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
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
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ConnectorDeletedTest.Profile.class)
public class ConnectorDeletedTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();

    @WireMockTestInstance
    com.github.tomakehurst.wiremock.WireMockServer server;

    @Test
    void statusIsUpdated() {
        final String clusterUrl = "/api/connector_mgmt/v1/kafka_connector_clusters/" + config.cluster().id();
        final String statusUrl = clusterUrl + "/deployments/" + DEPLOYMENT_ID + "/status";

        final Condition condition = new Condition(null, uid(), null, uid(), uid(), uid());
        final Operator operator = new Operator(uid(), "operator-type", "1.2.3");

        final ManagedConnector connector = Connectors.newConnector(
            config.cluster().id(),
            "connector-1",
            DEPLOYMENT_ID,
            Map.of());

        connector.getSpec().setOperatorSelector(new OperatorSelectorBuilder().withId(operator.getId()).build());

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(config.connectors().namespace())
            .create(connector);

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_READY)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(config.connectors().namespace())
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
            .inNamespace(config.connectors().namespace())
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
                    .inNamespace(config.connectors().namespace())
                    .withName(connector.getMetadata().getName())
                    .get()).isNull();
        });
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-sync-" + uid();

            return Map.of(
                "cos.cluster.id", uid(),
                "test.namespace", ns,
                "cos.connectors.namespace", ns,
                "cos.operators.namespace", ns,
                "cos.cluster.status.sync-interval", "disabled",
                "cos.connectors.poll-interval", "disabled",
                "cos.connectors.resync-interval", "disabled",
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
            MappingBuilder ready = WireMock.put(
                urlPathMatching("/api/connector_mgmt/v1/kafka_connector_clusters/.*/deployments/.*/status"))
                .withRequestBody(
                    matchingJsonPath("$[?($.phase == 'ready')])"));

            MappingBuilder deleted = WireMock.put(
                urlPathMatching("/api/connector_mgmt/v1/kafka_connector_clusters/.*/deployments/.*/status"))
                .withRequestBody(
                    matchingJsonPath("$[?($.phase == 'deleted')])"));

            server.stubFor(ready.willReturn(WireMock.ok()));
            server.stubFor(deleted.willReturn(WireMock.status(410)));

            return Map.of("control-plane-base-url", server.baseUrl());
        }

        @Override
        public void inject(TestInjector testInjector) {
            injectServerInstance(testInjector);
        }
    }
}
