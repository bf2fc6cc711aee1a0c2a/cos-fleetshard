package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTest
@TestProfile(CamelConnectorStatusBatchTest.Profile.class)
public class CamelConnectorStatusBatchTest extends CamelConnectorStatusTestSupport {
    @BeforeEach
    void setUp() {
        withCamelConnectorOperator("cm-1", "1.1.0");
    }

    @Test
    public void managedCamelConnectorStatusIsReportedByBatchTask() {
        final ConnectorDeployment cd = withDefaultConnectorDeployment();
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());

        awaitConnector(cd, connector -> {
            assertThat(connector).satisfies(c -> {
                assertThat(uc).hasSecret(
                    namespace,
                    c.getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion());
                assertThat(uc).hasKameletBinding(
                    namespace,
                    c.getMetadata().getName());
            });
        });

        updateKameletBinding(mandatoryGetManagedConnector(cd).getMetadata().getName());

        awaitStatus(clusterId, cd.getId(), status -> {
            assertThat(status.getPhase()).isEqualTo(DESIRED_STATE_READY);
        });
    }

    public static class Profile extends CamelTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.connectors.status.sync.interval", "1s",
                "cos.connectors.status.sync.all.interval", "1s");
        }
    }
}
