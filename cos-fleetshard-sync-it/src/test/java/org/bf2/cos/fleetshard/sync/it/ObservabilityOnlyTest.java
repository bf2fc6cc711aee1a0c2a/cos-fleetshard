package org.bf2.cos.fleetshard.sync.it;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.observability.v1.Observability;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(ObservabilityOnlyTest.Profile.class)
public class ObservabilityOnlyTest extends SyncTestSupport {

    @Test
    void observabilityIsProvisioned() {
        Observability observabilityResource = until(
            () -> Optional.ofNullable(kubernetesClient.resources(Observability.class)
                .inNamespace(config.observability().namespace())
                .withName(config.observability().resourceName())
                .get()),
            Objects::nonNull);

        Assertions.assertThat(observabilityResource)
            .matches(obs -> obs.getMetadata().getName().equals("observability-resource"))
            .matches(obs -> obs.getSpec().getClusterId().equals(config.cluster().id()))
            .matches(obs -> obs.getSpec().getStorage().getPrometheus().getVolumeClaimTemplate().getSpec().getResources()
                .getRequests()
                .equals(Map.of("storage", new IntOrString("50Gi"))));
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> configMap = new HashMap<>();
            configMap.put("cos.cluster.id", getId());
            configMap.put("test.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.observability.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.observability.enabled", "true");
            configMap.put("cos.observability.subscription.enabled", "false");
            configMap.put("cos.observability.resource-name", "observability-resource");
            configMap.put("cos.observability.storage-request", "50Gi");
            configMap.put("cos.observability.secrets-to-copy", "");
            configMap.put("cos.observability.config-maps-to-copy", "");
            configMap.put("cos.resources.update-interval", "disabled");
            configMap.put("cos.resources.poll-interval", "disabled");
            configMap.put("cos.resources.resync-interval", "disabled");
            configMap.put("cos.resources.housekeeper-interval", "disabled");
            configMap.put("cos.manager.sso-provider-refresh-timeout", "disabled");
            return configMap;
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(ObservabilityOnlyTest.FleetManagerTestResource.class));
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
