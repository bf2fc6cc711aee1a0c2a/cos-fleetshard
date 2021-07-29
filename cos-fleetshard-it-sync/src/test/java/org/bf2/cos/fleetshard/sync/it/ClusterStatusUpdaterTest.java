package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.it.InjectWireMock;
import org.bf2.cos.fleetshard.it.WireMockTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.ResourceUtil.uid;

@QuarkusTest
@TestProfile(ClusterStatusUpdaterTest.Profile.class)
public class ClusterStatusUpdaterTest extends SyncTestSupport {
    @InjectWireMock
    WireMockServer server;
    @ConfigProperty(name = "cluster-id")
    String clusterId;

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
            return Map.of(
                "cluster-id", uid(),
                "cos.cluster.status.sync.interval", "1s",
                "cos.connectors.poll.interval", "disabled",
                "cos.connectors.poll.resync.interval", "disabled",
                "cos.connectors.status.resync.interval", "disabled",
                "cos.connectors.status.sync.observe", "false");
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
