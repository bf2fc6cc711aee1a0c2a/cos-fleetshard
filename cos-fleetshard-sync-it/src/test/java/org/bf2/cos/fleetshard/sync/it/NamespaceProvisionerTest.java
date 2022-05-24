package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceProvisionerTest.Profile.class)
public class NamespaceProvisionerTest extends NamespaceProvisionerTestBase {
    @Inject
    FleetShardClient client;
    @Inject
    FleetShardSyncConfig config;

    @Test
    void namespaceIsProvisioned() {
        final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
        final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        Namespace ns1 = until(
            () -> fleetShardClient.getNamespace(deployment1),
            Objects::nonNull);

        assertThat(ns1).satisfies(item -> {
            assertThat(item.getMetadata().getName())
                .isEqualTo(client.generateNamespaceId(deployment1));
            assertThat(item.getMetadata().getName())
                .startsWith(config.tenancy().namespacePrefix());

            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_CLUSTER_ID, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_NAMESPACE_ID, deployment1)
                .containsEntry(Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_CREATED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_PART_OF, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE)
                .containsEntry(Resources.LABEL_KUBERNETES_INSTANCE, deployment1)
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, deployment1)
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_KIND, ConnectorNamespaceTenantKind.ORGANISATION.getValue())
                .containsKey(Resources.LABEL_NAMESPACE_TENANT_ID);
        });

        checkAddonPullSecretCopiedSuccessfullyToNamespace(ns1);

        Namespace ns2 = until(
            () -> fleetShardClient.getNamespace(deployment2),
            Objects::nonNull);

        assertThat(ns2).satisfies(item -> {
            assertThat(item.getMetadata().getName())
                .isEqualTo(client.generateNamespaceId(deployment2));

            assertThat(item.getMetadata().getLabels())
                .containsEntry(Resources.LABEL_CLUSTER_ID, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_NAMESPACE_ID, deployment2)
                .containsEntry(Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_CREATED_BY, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_PART_OF, fleetShardClient.getClusterId())
                .containsEntry(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE)
                .containsEntry(Resources.LABEL_KUBERNETES_INSTANCE, deployment2)
                .containsEntry(Resources.LABEL_KUBERNETES_NAME, "a--eval")
                .containsEntry(Resources.LABEL_NAMESPACE_TENANT_KIND, ConnectorNamespaceTenantKind.ORGANISATION.getValue())
                .containsKey(Resources.LABEL_NAMESPACE_TENANT_ID);
        });

        checkAddonPullSecretCopiedSuccessfullyToNamespace(ns2);
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.deployment.id.1", uid(),
                "test.deployment.id.2", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }
}
