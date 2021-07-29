package org.bf2.cos.fleetshard.operator.it;

import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.it.MetaServiceTestResource;
import org.bf2.cos.fleetshard.operator.it.support.CamelConnectorTestSupport;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.it.assertions.UnstructuredAssertions.assertThatUnstructured;
import static org.bf2.cos.fleetshard.support.ResourceUtil.uid;

@QuarkusTest
@TestProfile(CamelConnectorStopTest.Profile.class)
public class CamelConnectorStopTest extends CamelConnectorTestSupport {
    @Test
    void managedCamelConnectorIsStopped() {
        ManagedConnector mc1 = until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor);
            });

        this.fleetShard.edit(
            mc1.getMetadata().getName(),
            c -> c.getSpec().getDeployment().setDesiredState(DESIRED_STATE_STOPPED));

        ManagedConnector mc2 = until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Stopped);
            });

        assertThatUnstructured(uc).doesNotHaveSecret(
            namespace,
            mc2.getMetadata().getName() + "-" + mc2.getSpec().getDeployment().getDeploymentResourceVersion());
        assertThatUnstructured(uc).doesNotKameletBinding(
            namespace,
            mc2.getMetadata().getName());
    }

    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            return Map.of(
                "cos.cluster.id", uid(),
                // disable events to reduce noise
                "cos.connectors.watch.resources", "false",
                // disable events to reduce noise
                "cos.connectors.resync.interval", "disabled");
        }

        @Override
        protected List<TestResourceEntry> additionalTestResources() {
            return List.of(
                new TestResourceEntry(
                    MetaServiceTestResource.class,
                    Map.of(
                        "image", "quay.io/rhoas/cos-fleetshard-meta-camel:latest",
                        "prefix", "camel.meta.service")));
        }
    }
}
