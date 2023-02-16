package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.*;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.*;
import org.bf2.cos.fleetshard.sync.resources.ConnectorStatusUpdater;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.support.resources.Resources.*;

@QuarkusTest
@TestProfile(MetricsHousekeeperTest.Profile.class)
public class MetricsHousekeeperTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();
    public static final String CONNECTOR_ID = uid();

    @FleetManagerTestInstance
    FleetManagerMockServer server;
    @Inject
    MeterRegistry registry;

    @ConfigProperty(name = "test.namespace")
    String ns;

    @Test
    void metricsCleanUp() {
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
                .addToLabels("my.cos.bf2.org/connector-group", "baz")
                .addToAnnotations("cos.bf2.org/organization-id", "20000000")
                .addToAnnotations("cos.bf2.org/pricing-tier", "essential")
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(config.cluster().id())
                .withConnectorId(CONNECTOR_ID)
                .withDeploymentId(DEPLOYMENT_ID)
                .withOperatorSelector(new OperatorSelectorBuilder().withId(operator.getId()).build())
                .withDeployment(new DeploymentSpecBuilder()
                    .withConnectorTypeId("http_sync_v0.1")
                    .build())
                .build())
            .withStatus(new ManagedConnectorStatusBuilder().build())
            .build();

        kubernetesClient
            .resources(ManagedConnector.class)
            .inNamespace(ns)
            .resource(connector)
            .create();

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_READY)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        kubernetesClient
            .resource(connector)
            .inNamespace(ns)
            .replaceStatus();

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.version == '" + operator.getVersion() + "')]"))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.type == '" + operator.getType() + "')]"))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.id == '" + operator.getId() + "')]"))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'ready')]")));
        });

        assertMetrics(Boolean.FALSE);

        untilAsserted(() -> {
            assertThat(MetricsSupport.gauge(registry, config, ConnectorStatusUpdater.CONNECTOR_STATE))
                .isNotNull()
                .satisfies(gauge -> assertThat(gauge.value()).isEqualTo(1));
        });

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_DELETED)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        kubernetesClient
            .resource(connector)
            .inNamespace(ns)
            .replaceStatus();

        untilAsserted(() -> {
            assertThat(MetricsSupport.gauge(registry, config, ConnectorStatusUpdater.CONNECTOR_STATE))
                .isNotNull()
                .satisfies(gauge -> assertThat(gauge.value()).isEqualTo(3));
        });

        untilAsserted(() -> {
            assertThat(
                MetricsSupport.counter(registry, "deletion_timestamp", config, ConnectorStatusUpdater.CONNECTOR_STATE_COUNT))
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isGreaterThan(0));
        });

        // The following test expect the Metrics Housekeeper job has executed
        untilAsserted(() -> {
            assertThat(
                MetricsSupport.gauge(registry, config, ConnectorStatusUpdater.CONNECTOR_STATE))
                .isNull();
        });

        untilAsserted(() -> {
            assertThat(
                MetricsSupport.counter(registry, config, ConnectorStatusUpdater.CONNECTOR_STATE_COUNT))
                .isNull();
        });

        assertMetrics(Boolean.TRUE);
    }

    void assertMetrics(Boolean metricsDeleted) {
        Map<String, String> expectedTags = Map.of(
            "connector_group", "baz",
            "organization_id", "20000000");

        List<Meter> meters = registry.getMeters();
        assertThat(meters).isNotEmpty();

        String regex = "cos.fleetshard.sync.connector.state\\..*";
        int matches = 0;

        for (Meter meter : meters) {
            if (!meter.getId().getName().matches(regex)) {
                continue;
            }

            matches++;
        }

        if (metricsDeleted) {
            assertThat(matches)
                .withFailMessage(() -> String.format("Found meters matching '%s'", regex))
                .isEqualTo(0);
        } else {
            assertThat(matches)
                .withFailMessage(() -> String.format("No meters matching '%s'", regex))
                .isGreaterThan(0);
        }
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "1s",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.metrics.recorder.tags.labels[0]", "my.cos.bf2.org/connector-group",
                "cos.metrics.recorder.tags.annotations[0]", "cos.bf2.org/organization-id",
                "cos.resources.metrics-housekeeper-interval", "10s",
                "cos.resources.metrics-housekeeper-delete-metrics-after", "5s");
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
            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList());
                });

            server.stubMatching(
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces/.*/status",
                () -> WireMock.ok());

            server.stubMatching(
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/status",
                () -> WireMock.ok());
        }
    }
}
