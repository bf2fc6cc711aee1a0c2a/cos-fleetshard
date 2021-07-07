package org.bf2.cos.fleetshard.operator.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.client.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorStopTest extends CamelTestSupport {
    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;

    @Test
    void managedCamelConnectorStatusIsReported() {
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        final ManagedConnectorOperator op = withConnectorOperator("cm-1", "1.1.0", camelMeta);
        final ConnectorDeployment cd = withDefaultConnectorDeployment();

        await(() -> {
            ConnectorDeploymentStatus status = fm.getConnectorDeploymentStatus(clusterId, cd.getId());
            List<ManagedConnector> managedConnectors = getManagedConnectors(cd);

            if (managedConnectors.isEmpty()) {
                return false;
            }

            return status != null
                && Objects.equals("provisioning", status.getPhase())
                && managedConnectors.get(0).getStatus() != null
                && managedConnectors.get(0).getStatus().getResources() != null;
        });

        var resources = new ArrayList<>(getManagedConnectors(cd).get(0).getStatus().getResources());

        fm.updateConnector(clusterId, cd.getId(), c -> {
            c.getDeployment().getSpec().setDesiredState(DESIRED_STATE_STOPPED);
        });

        await(() -> {
            return Objects.equals(
                DESIRED_STATE_STOPPED,
                fm.getConnectorDeploymentStatus(clusterId, cd.getId()).getPhase());
        });

        await(() -> {
            return resources.stream().noneMatch(r -> uc.getAsNode(namespace, r) != null);
        });

        fm.updateConnector(clusterId, cd.getId(), c -> {
            c.getDeployment().getSpec().setDesiredState(DESIRED_STATE_READY);
        });

        await(() -> {
            ConnectorDeploymentStatus status = fm.getConnectorDeploymentStatus(clusterId, cd.getId());
            List<ManagedConnector> managedConnectors = getManagedConnectors(cd);

            if (managedConnectors.isEmpty()) {
                return false;
            }

            return status != null
                && Objects.equals("provisioning", status.getPhase())
                && managedConnectors.get(0).getStatus() != null
                && managedConnectors.get(0).getStatus().getResources() != null;
        });
    }
}
