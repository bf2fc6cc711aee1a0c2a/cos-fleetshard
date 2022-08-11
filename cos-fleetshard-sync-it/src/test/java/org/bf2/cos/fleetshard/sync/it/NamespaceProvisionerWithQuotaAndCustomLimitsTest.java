package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceQuota;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.CollectionUtils.mapOf;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceProvisionerWithQuotaAndCustomLimitsTest.Profile.class)
public class NamespaceProvisionerWithQuotaAndCustomLimitsTest extends SyncTestSupport {
    @Inject
    FleetShardClient client;

    @Test
    void namespaceIsProvisioned() {
        final Config cfg = ConfigProvider.getConfig();
        final String nsId1 = cfg.getValue("test.ns.id.1", String.class);
        final NamespacedName pullSecret = new NamespacedName(client.generateNamespaceId(nsId1), config.imagePullSecretsName());

        RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        Namespace ns = until(
            () -> fleetShardClient.getNamespace(nsId1),
            Objects::nonNull);

        assertThat(ns).satisfies(item -> {
            assertThat(item.getMetadata().getName())
                .isEqualTo(client.generateNamespaceId(nsId1));

            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_CLUSTER_ID, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_NAMESPACE_ID, nsId1)
                .containsEntry(Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_CREATED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_PART_OF, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE)
                .containsEntry(Resources.LABEL_KUBERNETES_INSTANCE, nsId1)
                .containsKey(Resources.LABEL_UOW);

            assertThat(item.getMetadata().getAnnotations())
                .containsEntry(Resources.ANNOTATION_NAMESPACE_QUOTA, "true");
        });

        until(
            () -> fleetShardClient.getSecret(pullSecret).filter(ps -> {
                return Objects.equals(
                    ps.getMetadata().getLabels().get(Resources.LABEL_UOW),
                    ns.getMetadata().getLabels().get(Resources.LABEL_UOW));
            }),
            Objects::nonNull);

        untilAsserted(
            () -> {
                return Optional.ofNullable(
                    fleetShardClient.getKubernetesClient()
                        .limitRanges()
                        .inNamespace(ns.getMetadata().getName())
                        .withName(ns.getMetadata().getName() + "-limits")
                        .get());
            },
            lr -> {
                assertThat(lr).satisfies(item -> {
                    assertThat(item.getMetadata().getLabels())
                        .containsEntry(Resources.LABEL_UOW, ns.getMetadata().getLabels().get(Resources.LABEL_UOW));
                    assertThat(item.getSpec().getLimits())
                        .hasSize(1);

                    assertThat(item.getSpec().getLimits().get(0).getDefault())
                        .describedAs("LimitRanges (limits)")
                        .containsEntry(
                            ConnectorNamespaceProvisioner.LIMITS_CPU,
                            cfg.getValue("cos.quota.default-limits.cpu", Quantity.class))
                        .containsEntry(
                            ConnectorNamespaceProvisioner.LIMITS_MEMORY,
                            cfg.getValue("cos.quota.default-limits.memory", Quantity.class));

                    assertThat(item.getSpec().getLimits().get(0).getDefaultRequest())
                        .describedAs("LimitRanges (request)")
                        .containsEntry(
                            ConnectorNamespaceProvisioner.LIMITS_CPU,
                            cfg.getValue("cos.quota.default-request.cpu", Quantity.class))
                        .containsEntry(
                            ConnectorNamespaceProvisioner.LIMITS_MEMORY,
                            cfg.getValue("cos.quota.default-request.memory", Quantity.class));
                });
            });

        ResourceQuota rq = until(
            () -> {
                ResourceQuota answer = fleetShardClient.getKubernetesClient()
                    .resourceQuotas()
                    .inNamespace(ns.getMetadata().getName())
                    .withName(ns.getMetadata().getName() + "-quota")
                    .get();

                return Optional.ofNullable(answer);
            },
            Objects::nonNull);

        assertThat(rq).satisfies(item -> {
            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_UOW, ns.getMetadata().getLabels().get(Resources.LABEL_UOW));
            assertThat(item.getSpec().getHard())
                .containsEntry(
                    ConnectorNamespaceProvisioner.RESOURCE_QUOTA_LIMITS_CPU,
                    new Quantity(cfg.getValue("test.ns.id.1.limits.cpu", String.class)))
                .containsEntry(
                    ConnectorNamespaceProvisioner.RESOURCE_QUOTA_REQUESTS_CPU,
                    new Quantity(cfg.getValue("test.ns.id.1.requests.cpu", String.class)))
                .containsEntry(
                    ConnectorNamespaceProvisioner.RESOURCE_QUOTA_LIMITS_MEMORY,
                    new Quantity(cfg.getValue("test.ns.id.1.limits.memory", String.class)))
                .containsEntry(
                    ConnectorNamespaceProvisioner.RESOURCE_QUOTA_REQUESTS_MEMORY,
                    new Quantity(cfg.getValue("test.ns.id.1.requests.memory", String.class)));
        });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return mapOf(
                "test.ns.id.1", uid(),
                "test.ns.id.1.limits.cpu", "0.2",
                "test.ns.id.1.requests.cpu", "01",
                "test.ns.id.1.limits.memory", "20m",
                "test.ns.id.1.requests.memory", "10m",
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "disabled",
                "cos.quota.default-limits.cpu", "401m",
                "cos.quota.default-limits.memory", "402m",
                "cos.quota.default-request.cpu", "301m",
                "cos.quota.default-request.memory", "302m");
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
            final Config cfg = ConfigProvider.getConfig();
            final String nsId1 = cfg.getValue("test.ns.id.1", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    var ns = namespace(nsId1, nsId1, n -> {
                        n.status(new ConnectorNamespaceStatus().state(ConnectorNamespaceState.READY).connectorsDeployed(0));
                        n.tenant(new ConnectorNamespaceTenant().id(uid()).kind(ConnectorNamespaceTenantKind.ORGANISATION));
                        n.quota(new ConnectorNamespaceQuota()
                            .cpuLimits(cfg.getValue("test.ns.id.1.limits.cpu", String.class))
                            .cpuRequests(cfg.getValue("test.ns.id.1.requests.cpu", String.class))
                            .memoryLimits(cfg.getValue("test.ns.id.1.limits.memory", String.class))
                            .memoryRequests(cfg.getValue("test.ns.id.1.requests.memory", String.class)));
                    });

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList(ns));
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());
        }
    }
}
