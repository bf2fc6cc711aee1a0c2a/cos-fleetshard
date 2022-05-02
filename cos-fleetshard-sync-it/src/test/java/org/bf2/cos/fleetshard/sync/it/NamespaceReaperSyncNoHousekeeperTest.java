package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestInstance;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceReaperSyncNoHousekeeperTest.Profile.class)
public class NamespaceReaperSyncNoHousekeeperTest extends NamespaceReaperSyncTestBase {

    @WireMockTestInstance
    WireMockServer server;

    @Test
    void namespaceIsProvisioned() {
        final String ns1 = ConfigProvider.getConfig().getValue("test.ns.id.1", String.class);
        final String ns2 = ConfigProvider.getConfig().getValue("test.ns.id.2", String.class);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(1L)
            .post("/test/provisioner/namespaces");

        until(
            () -> fleetShardClient.getNamespace(ns1),
            Objects::nonNull);
        until(
            () -> fleetShardClient.getNamespace(ns2),
            Objects::nonNull);

        given()
            .contentType(MediaType.TEXT_PLAIN)
            .body(0L)
            .post("/test/provisioner/namespaces");

        until(
            () -> fleetShardClient.getNamespace(ns1),
            n -> {
                return Namespaces.PHASE_READY.equals(Resources.getLabel(n, Resources.LABEL_NAMESPACE_STATE))
                    && null == Resources.getLabel(n, Resources.LABEL_NAMESPACE_STATE_FORCED);
            });

        until(
            () -> fleetShardClient.getNamespace(ns2),
            n -> {
                return Namespaces.PHASE_DELETED.equals(Resources.getLabel(n, Resources.LABEL_NAMESPACE_STATE))
                    && "true".equals(Resources.getLabel(n, Resources.LABEL_NAMESPACE_STATE_FORCED));
            });
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.ns.id.1", uid(),
                "test.ns.id.2", uid(),
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(OidcTestResource.class),
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }
}
