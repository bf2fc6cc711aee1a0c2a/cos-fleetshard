package org.bf2.cos.fleetshard.operator.it;

import java.util.ArrayList;
import java.util.Optional;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorDeleteTest extends CamelTestSupport {
    @Test
    void managedCamelConnectorIsDeleted() {
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        final ManagedConnectorOperator op = withCamelConnectorOperator("cm-1", "1.1.0");
        final ConnectorDeployment cd = withDefaultConnectorDeployment();

        awaitStatus(clusterId, cd.getId(), status -> {
            assertThat(status.getPhase()).isEqualTo("provisioning");

            Optional<ManagedConnector> mc = getManagedConnector(cd);
            assertThat(mc).isPresent();
            assertThat(mc.get()).hasStatus();
            assertThat(mc.get()).hasResources();
        });

        var resources = new ArrayList<>(mandatoryGetManagedConnector(cd).getStatus().getResources());

        updateConnector(clusterId, cd.getId(), c -> {
            c.getDeployment().getSpec().setDesiredState(DESIRED_STATE_DELETED);
        });
        awaitStatus(clusterId, cd.getId(), status -> {
            assertThat(status.getPhase()).isEqualTo(DESIRED_STATE_DELETED);
        });

        await(() -> {
            assertThat(resources)
                .noneMatch(r -> uc.getAsNode(namespace, r) != null);
        });

    }
}
