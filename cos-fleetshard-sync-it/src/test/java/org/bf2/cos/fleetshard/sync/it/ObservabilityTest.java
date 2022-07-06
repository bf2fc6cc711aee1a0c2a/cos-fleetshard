package org.bf2.cos.fleetshard.sync.it;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(ObservabilityTest.Profile.class)
public class ObservabilityTest extends SyncTestSupport {

    @Test
    void observabilityIsProvisioned() {
        until(
            () -> fleetShardClient.getObservability(),
            obs -> obs.getMetadata().getName().equals("rhoc-observability-stack"));

        until(
            () -> fleetShardClient.getObservability(),
            obs -> obs.getSpec().getClusterId().equals(config.cluster().id()));

        until(
            () -> fleetShardClient.getObservability(),
            obs -> obs.getSpec().getStorage().getPrometheus().getVolumeClaimTemplate().getSpec().getResources().getRequests()
                .equals(Map.of("storage", new IntOrString("100Gi"))));
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.observability.enabled", "true",
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "disabled",
                "cos.manager.sso-provider-refresh-timeout", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(ObservabilityTest.FleetManagerTestResource.class));
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
        }
    }

}
