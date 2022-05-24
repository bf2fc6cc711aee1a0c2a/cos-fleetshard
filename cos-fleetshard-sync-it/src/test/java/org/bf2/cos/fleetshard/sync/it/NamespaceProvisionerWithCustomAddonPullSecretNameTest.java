package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.TestFleetShardSync;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceProvisionerWithCustomAddonPullSecretNameTest.Profile.class)
public class NamespaceProvisionerWithCustomAddonPullSecretNameTest extends NamespaceProvisionerTestBase {

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

        checkAddonPullSecretCopiedSuccessfullyToNamespace(ns1);

        Namespace ns2 = until(
            () -> fleetShardClient.getNamespace(deployment2),
            Objects::nonNull);

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
                "cos.resources.resync-interval", "disabled",
                "cos.image-pull-secrets-name", TestFleetShardSync.CUSTOM_ADDON_PULL_SECRET_NAME);
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }
}
