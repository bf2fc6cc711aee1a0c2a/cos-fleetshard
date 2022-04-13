package org.bf2.cos.fleetshard.sync.it;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.restassured.RestAssured;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class NamespaceProvisionerWithNoQuotaTestBase extends SyncTestSupport {
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
                .containsEntry(Resources.ANNOTATION_NAMESPACE_QUOTA, "false");
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
                ResourceQuota answer = fleetShardClient.getKubernetesClient()
                    .resourceQuotas()
                    .inNamespace(ns.getMetadata().getName())
                    .withName(ns.getMetadata().getName() + "-quota")
                    .get();

                assertThat(answer).isNull();
            });
    }
}
