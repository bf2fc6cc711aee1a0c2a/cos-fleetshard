package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.it.resources.BaseTestProfile;
import org.bf2.cos.fleetshard.it.resources.WireMockTestInstance;
import org.bf2.cos.fleetshard.it.resources.WireMockTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(ClusterStatusUpdaterTest.Profile.class)
public class ClusterStatusUpdaterTest extends SyncTestSupport {
    @WireMockTestInstance
    com.github.tomakehurst.wiremock.WireMockServer server;

    @Test
    void statusIsUpdated() {
        final String statusUrl = "/api/connector_mgmt/v1/kafka_connector_clusters/" + clusterId + "/status";

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'ready')]")));

        });
    }

    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            final String ns = "cos-" + uid();

            return Map.of(
                "cos.cluster.id", uid(),
                "test.namespace", ns,
                "cos.connectors.namespace", ns,
                "cos.operators.namespace", ns,
                "cos.cluster.status.sync.interval", "1s",
                "cos.connectors.poll.interval", "disabled",
                "cos.connectors.poll.resync.interval", "disabled",
                "cos.connectors.status.resync.interval", "disabled",
                "cos.connectors.status.sync.observe", "false");
        }

        @Override
        protected List<TestResourceEntry> additionalTestResources() {
            return List.of(
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected Map<String, String> doStart(com.github.tomakehurst.wiremock.WireMockServer server) {
            MappingBuilder request = WireMock.put(WireMock.urlPathMatching(
                "/api/connector_mgmt/v1/kafka_connector_clusters/.*/status"));

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
