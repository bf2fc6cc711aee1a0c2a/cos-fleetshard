package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.config.v1.ClusterVersion;
import io.fabric8.openshift.api.model.config.v1.ClusterVersionBuilder;
import io.fabric8.openshift.api.model.config.v1.ClusterVersionSpecBuilder;
import io.fabric8.openshift.api.model.config.v1.ClusterVersionStatus;
import io.fabric8.openshift.api.model.config.v1.ReleaseBuilder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(ClusterStatusUpdaterTest.Profile.class)
public class ClusterStatusUpdaterTest extends SyncTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;

    @Test
    void statusIsUpdated() {
        final String statusUrl = "/api/connector_mgmt/v1/agent/kafka_connector_clusters/" + config.cluster().id() + "/status";
        final String clusterId = ConfigProvider.getConfig().getValue("test.openshift.cluster.id", String.class);
        final String clusterVersion = ConfigProvider.getConfig().getValue("test.openshift.cluster.version", String.class);

        untilAsserted(() -> {
            server.verify(putRequestedFor(urlEqualTo(statusUrl))
                .withHeader(ContentTypeHeader.KEY, equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$[?($.platform.id == '" + clusterId + "')]"))
                .withRequestBody(matchingJsonPath("$[?($.platform.version == '" + clusterVersion + "')]"))
                .withRequestBody(matchingJsonPath("$[?($.platform.type == 'OpenShift')]"))
                .withRequestBody(matchingJsonPath("$[?($.phase == 'ready')]")));
        });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.update-interval", "1s");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(ClusterVersionTestResource.class),
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
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/status",
                () -> WireMock.ok());
        }
    }

    public static class ClusterVersionTestResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            final String clusterId = UUID.randomUUID().toString();
            final String clusterVersion = "4.10.1";

            try (KubernetesClient client = new DefaultKubernetesClient()) {
                ClusterVersion res = new ClusterVersionBuilder()
                    .withMetadata(new ObjectMetaBuilder()
                        .withName("version")
                        .build())
                    .withSpec(new ClusterVersionSpecBuilder()
                        .withClusterID(clusterId)
                        .build())
                    .build();

                client.resources(ClusterVersion.class)
                    .withName("version")
                    .createOrReplace(res);

                client.resources(ClusterVersion.class)
                    .withName("version").editStatus(cv -> {
                        if (cv.getStatus() == null) {
                            cv.setStatus(new ClusterVersionStatus());
                        }

                        cv.getStatus().setObservedGeneration(1L);
                        cv.getStatus().setVersionHash(clusterId);
                        cv.getStatus().setAvailableUpdates(List.of(new ReleaseBuilder()
                            .withVersion(clusterVersion)
                            .build()));
                        cv.getStatus().setDesired(
                            new ReleaseBuilder()
                                .withVersion(clusterVersion)
                                .build());

                        return cv;
                    });
            }

            return Map.of(
                "test.openshift.cluster.id", clusterId,
                "test.openshift.cluster.version", clusterVersion);
        }

        @Override
        public void stop() {
            try (KubernetesClient client = new DefaultKubernetesClient()) {
                client.resources(ClusterVersion.class)
                    .withName("version")
                    .delete();
            }
        }
    }
}
