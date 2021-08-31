package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.it.InjectWireMock;
import org.bf2.cos.fleetshard.it.WireMockTestResource;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.fabric8.kubernetes.api.model.Condition;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.api.ManagedConnector.CONTEXT_DEPLOYMENT;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_RESOURCE_CONTEXT;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ConnectorStatusUpdaterTest.Profile.class)
public class ConnectorStatusUpdaterTest extends SyncTestSupport {
    public static final String DEPLOYMENT_ID = uid();

    @InjectWireMock
    WireMockServer server;

    @Test
    void statusIsUpdated() {
        final String clusterId = ConfigProvider.getConfig().getValue("cluster-id", String.class);
        final String clusterUrl = "/api/connector_mgmt/v1/kafka_connector_clusters/" + clusterId;
        final String statusUrl = clusterUrl + "/deployments/" + DEPLOYMENT_ID + "/status";
        final String name = Connectors.generateConnectorId();

        final Condition condition = new Condition(null, uid(), null, uid(), uid(), uid());
        final Operator operator = new Operator(uid(), "operator-type", "1.2.3");

        final ManagedConnector connector = Connectors.newConnector(
            name,
            clusterId,
            "connector-1",
            DEPLOYMENT_ID,
            Map.of(LABEL_RESOURCE_CONTEXT, CONTEXT_DEPLOYMENT));

        connector.getSpec().setOperatorSelector(new OperatorSelectorBuilder().withId(operator.getId()).build());

        ksrv.getClient()
            .resources(ManagedConnector.class)
            .inNamespace(namespace)
            .create(connector);

        connector.getStatus().setConnectorStatus(new ConnectorStatusSpecBuilder()
            .withPhase(DESIRED_STATE_READY)
            .withConditions(condition)
            .withAssignedOperator(operator)
            .build());

        ksrv.getClient()
            .resources(ManagedConnector.class)
            .inNamespace(namespace)
            .withName(name)
            .replaceStatus(connector);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.version == '" + operator.getVersion() + "')]"))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.type == '" + operator.getType() + "')]"))
                .withRequestBody(matchingJsonPath("$.operators.assigned[?(@.id == '" + operator.getId() + "')]"))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'ready')]")));
        });
    }

    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            return Map.of(
                "cluster-id", uid(),
                "cos.cluster.status.sync.interval", "disabled",
                "cos.connectors.poll.interval", "disabled",
                "cos.connectors.poll.resync.interval", "disabled",
                "cos.connectors.status.resync.interval", "1s");
        }

        @Override
        protected List<TestResourceEntry> additionalTestResources() {
            return List.of(new TestResourceEntry(FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected Map<String, String> doStart(WireMockServer server) {
            MappingBuilder request = WireMock.put(WireMock.urlPathMatching(
                "/api/connector_mgmt/v1/kafka_connector_clusters/.*/deployments/.*/status"));

            ResponseDefinitionBuilder response = WireMock.ok();

            server.stubFor(request.willReturn(response));

            return Map.of("control-plane-base-url", server.baseUrl());
        }

        @Override
        public void inject(TestInjector testInjector) {
            injectServerInstance(testInjector);
        }
    }
}
