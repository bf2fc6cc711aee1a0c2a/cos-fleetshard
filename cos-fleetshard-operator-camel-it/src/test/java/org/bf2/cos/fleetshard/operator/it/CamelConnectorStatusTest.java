package org.bf2.cos.fleetshard.operator.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.it.support.CamelConnectorTestSupport;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.it.assertions.UnstructuredAssertions.assertThatUnstructured;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(CamelConnectorStatusTest.Profile.class)
public class CamelConnectorStatusTest extends CamelConnectorTestSupport {
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
            connector.getMetadata().getName() + Connectors.CONNECTOR_SECRET_SUFFIX);
        assertThatUnstructured(uc).hasKameletBinding(
            namespace,
            connector.getMetadata().getName());

        until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor);
            });

        editUnstructuredStatus(
            KameletBinding.RESOURCE_API_VERSION,
            KameletBinding.RESOURCE_KIND,
            connector.getMetadata().getName(),
            binding -> {
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
                "cos.operator.id", uid(),
                "cos.operator.version", "1.5.0",
                "cos.connectors.watch.resources", "true",
                // disable events to reduce noise
                "cos.connectors.resync.interval", "disabled");
        }
    }
}
