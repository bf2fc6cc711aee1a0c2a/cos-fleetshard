package org.bf2.cos.fleetshard.operator.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.it.MetaServiceTestResource;
import org.bf2.cos.fleetshard.operator.it.support.CamelConnectorTestSupport;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.it.assertions.UnstructuredAssertions.assertThatUnstructured;
import static org.bf2.cos.fleetshard.support.ResourceUtil.uid;

@QuarkusTest
@TestProfile(CamelConnectorStatusTest.Profile.class)
public class CamelConnectorStatusTest extends CamelConnectorTestSupport {
    //@Disabled("https://github.com/fabric8io/kubernetes-client/issues/3379")
    @Test
    void managedCamelConnectorStatusIsUpdated() {
        until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor);
            });

        assertThatUnstructured(uc).hasSecret(
            namespace,
            connector.getMetadata().getName() + "-" + connector.getSpec().getDeployment().getDeploymentResourceVersion());
        assertThatUnstructured(uc).hasKameletBinding(
            namespace,
            connector.getMetadata().getName());

        updateUnstructured(
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            connector.getMetadata().getName(), binding -> {
                Map<String, Object> status = (Map<String, Object>) binding.getAdditionalProperties().get("status");
                if (status == null) {
                    status = new HashMap<>();
                }
                status.put("phase", "Ready");
                status.put("conditions", List.of(Map.of(
                    "message", "a message",
                    "reason", "a reason",
                    "status", "the status",
                    "type", "the type",
                    "lastTransitionTime", "2021-06-12T12:35:09+02:00")));

                binding.getAdditionalProperties().put("status", status);
            });

        until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor)
                    && item.getStatus().getConnectorStatus().isInPhase(DESIRED_STATE_READY)
                    && !item.getStatus().getConnectorStatus().getConditions().isEmpty();
            });
    }

    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            return Map.of(
                "cos.cluster.id", uid(),
                // disable events to reduce noise
                "cos.connectors.watch.resources", "true",
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
