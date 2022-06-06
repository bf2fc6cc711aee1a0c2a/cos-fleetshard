package org.bf2.cos.fleetshard.sync.it;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerTestInstance;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Namespace;
import io.micrometer.core.instrument.MeterRegistry;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespaceReaperDelReprovisionTestBase extends NamespaceReaperTestSupport {
    @FleetManagerTestInstance
    FleetManagerMockServer server;
    @Inject
    MeterRegistry registry;

    @Test
    void namespaceIsProvisioned() {
        final String deploymentId = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(
                registry.find(config.metrics().baseName() + ConnectorNamespaceProvisioner.METRICS_SUFFIX + ".count").counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1));
        });

        Namespace ns1 = until(
            () -> fleetShardClient.getNamespace(deploymentId),
            Objects::nonNull);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(
                registry.find(config.metrics().baseName() + ConnectorNamespaceProvisioner.METRICS_SUFFIX + ".count").counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(2));
        });

        Namespace ns2 = until(
            () -> fleetShardClient.getNamespace(deploymentId),
            Objects::nonNull);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        untilAsserted(() -> {
            assertThat(
                registry.find(config.metrics().baseName() + ConnectorNamespaceProvisioner.METRICS_SUFFIX + ".count").counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(3));
        });

        Namespace ns3 = until(
            () -> fleetShardClient.getNamespace(deploymentId),
            Objects::nonNull);

        assertThat(ns1.getMetadata().getLabels())
            .containsKey(Resources.LABEL_UOW);
        assertThat(ns2.getMetadata().getLabels())
            .containsKey(Resources.LABEL_UOW);
        assertThat(ns3.getMetadata().getLabels())
            .containsKey(Resources.LABEL_UOW);

        assertThat(ns2.getMetadata().getLabels().get(Resources.LABEL_UOW))
            .isNotEqualTo(ns1.getMetadata().getLabels().get(Resources.LABEL_UOW));
        assertThat(ns3.getMetadata().getLabels().get(Resources.LABEL_UOW))
            .isNotEqualTo(ns2.getMetadata().getLabels().get(Resources.LABEL_UOW));
    }
}
