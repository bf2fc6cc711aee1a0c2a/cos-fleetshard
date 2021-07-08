package org.bf2.cos.fleetshard.operator.it;

import java.util.ArrayList;
import java.util.Objects;
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
            Optional<ManagedConnector> mc = getManagedConnector(cd);
            if (mc.isEmpty()) {
                return false;
            }

            return Objects.equals("provisioning", status.getPhase())
                && mc.get().getStatus() != null
                && mc.get().getStatus().getResources() != null;
        });

        var resources = new ArrayList<>(mandatoryGetManagedConnector(cd).getStatus().getResources());

        updateConnector(clusterId, cd.getId(), c -> {
            c.getDeployment().getSpec().setDesiredState(DESIRED_STATE_DELETED);
        });
        awaitStatus(clusterId, cd.getId(), status -> {
            return Objects.equals(DESIRED_STATE_DELETED, status.getPhase());
        });

        await(() -> {
            for (var resource : resources) {
                if (uc.getAsNode(namespace, resource) != null) {
                    return false;
                }
            }

            return true;
        });

    }
}
