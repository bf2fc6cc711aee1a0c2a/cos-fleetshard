package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceReaperDeletedReprovisionTest.Profile.class)
public class NamespaceReaperDeletedReprovisionTest extends NamespaceReaperDelReprovisionTestBase {

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.namespace.delete.state", ConnectorNamespaceState.DELETED.getValue(),
                "test.deployment.connectors", "1",
                "test.deployment.id", uid(),
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
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }
}
