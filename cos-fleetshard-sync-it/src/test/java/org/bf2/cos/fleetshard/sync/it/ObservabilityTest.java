package org.bf2.cos.fleetshard.sync.it;

import java.util.*;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.TestFleetShardSync;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.observability.v1.Observability;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(ObservabilityTest.Profile.class)
public class ObservabilityTest extends SyncTestSupport {

    public static Map<String, String> secretData() {
        Map<String, String> data = new HashMap<>();
        data.put("token", "ZGF0YSBmb3IgYSBzZWNyZXQ=");

        return data;
    }

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

        Secret observabilitySecret = until(
            () -> Optional.ofNullable(kubernetesClient.resources(Secret.class)
                .inNamespace(config.namespace())
                .withName(config.observability().observatoriumSecretName())
                .get()),
            Objects::nonNull);

        Assertions.assertThat((observabilitySecret))
            .matches(secret -> secret.getData().equals(secretData()));
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
            configMap.put("cos.observability.environment", "production");
            configMap.put("cos.observability.observatorium-secret-name", "observatorium-configuration-red-hat-sso");
            configMap.put("cos.observability.resource-name", "observability-resource");
            configMap.put("cos.observability.storage-request", "50Gi");
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

    @Mock
    @Priority(Integer.MAX_VALUE)
    @ApplicationScoped
    static class OurFleetShardSync extends TestFleetShardSync {

        @Inject
        KubernetesClient client;

        @ConfigProperty(name = "test.namespace")
        String namespace;

        @ConfigProperty(name = "cos.observability.observatorium-secret-name")
        String secretName;

        @ConfigProperty(name = "cos.observability.environment")
        String environment;

        @Override
        public void beforeStart() {
            final Secret productionSecret = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(secretName + "-" + environment)
                    .withNamespace(namespace)
                    .build())
                .withData(secretData())
                .build();

            client.resource(productionSecret)
                .inNamespace(namespace)
                .createOrReplace();
        }

    }

}
