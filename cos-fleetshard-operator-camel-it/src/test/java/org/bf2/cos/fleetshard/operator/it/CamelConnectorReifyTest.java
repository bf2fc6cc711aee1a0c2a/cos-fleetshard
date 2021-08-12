package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.it.BaseTestProfile;
import org.bf2.cos.fleetshard.operator.it.support.CamelConnectorTestSupport;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.bf2.cos.fleetshard.it.assertions.UnstructuredAssertions.assertThatUnstructured;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(CamelConnectorReifyTest.Profile.class)
public class CamelConnectorReifyTest extends CamelConnectorTestSupport {
    @Test
    void managedCamelConnectorIsReified() {
        ManagedConnector mc = until(
            () -> getConnectorByDeploymentId(connector.getSpec().getDeploymentId()),
            item -> {
                return item.getSpec().getDeployment().getDeploymentResourceVersion() == 1L
                    && item.getStatus().isInPhase(ManagedConnectorStatus.PhaseType.Monitor);
            });

        assertThatUnstructured(uc).hasSecretSatisfying(
            namespace,
            mc.getMetadata().getName() + "-camel-" + mc.getSpec().getDeployment().getDeploymentResourceVersion(),
            resource -> {
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/watch']")
                    .isString()
                    .isEqualTo("true");
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/cluster.id']")
                    .isString()
                    .isEqualTo(clusterId);
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/connector.id']")
                    .isString()
                    .isEqualTo(connector.getSpec().getConnectorId());
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/deployment.id']")
                    .isString()
                    .isEqualTo(connector.getSpec().getDeploymentId());
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/deployment.resource.version']")
                    .isString()
                    .isEqualTo("1");

                assertThatJson(resource)
                    .inPath("$.metadata.ownerReferences[0].apiVersion")
                    .isEqualTo(ManagedConnector.API_VERSION);
                assertThatJson(resource)
                    .inPath("$.metadata.ownerReferences[0].kind")
                    .isEqualTo(ManagedConnector.class.getSimpleName());
            });

        assertThatUnstructured(uc).hasKameletBindingSatisfying(
            namespace,
            mc.getMetadata().getName() + "-camel",
            resource -> {
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/watch']")
                    .isString()
                    .isEqualTo("true");
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/cluster.id']")
                    .isString()
                    .isEqualTo(clusterId);
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/connector.id']")
                    .isString()
                    .isEqualTo(connector.getSpec().getConnectorId());
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/deployment.id']")
                    .isString()
                    .isEqualTo(connector.getSpec().getDeploymentId());
                assertThatJson(resource)
                    .inPath("$.metadata.labels['cos.bf2.org/deployment.resource.version']")
                    .isString()
                    .isEqualTo("1");

                assertThatJson(resource)
                    .inPath("$.metadata.ownerReferences[0].apiVersion")
                    .isEqualTo(ManagedConnector.API_VERSION);
                assertThatJson(resource)
                    .inPath("$.metadata.ownerReferences[0].kind")
                    .isEqualTo(ManagedConnector.class.getSimpleName());
            });
    }

    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            return Map.of(
                "cos.cluster.id", uid(),
                "cos.operator.id", uid(),
                "cos.operator.version", "1.5.0",
                // disable events to reduce noise
                "cos.connectors.watch.resources", "false",
                // disable events to reduce noise
                "cos.connectors.resync.interval", "disabled");
        }
    }
}
