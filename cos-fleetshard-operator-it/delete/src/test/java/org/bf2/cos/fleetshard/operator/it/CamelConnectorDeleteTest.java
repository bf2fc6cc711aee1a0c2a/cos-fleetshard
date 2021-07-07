package org.bf2.cos.fleetshard.operator.it;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.client.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorDeleteTest extends CamelTestSupport {
    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;
    @ConfigProperty(
        name = "test.namespace")
    String namespace;

    @Test
    void managedCamelConnectorStatusIsReported() {
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        final ManagedConnectorOperator op = withConnectorOperator("cm-1", "1.1.0", camelMeta);
        final ConnectorDeployment cd = withDefaultConnectorDeployment();

        await(30, TimeUnit.SECONDS, () -> {
            ConnectorDeploymentStatus status = fm.getCluster(clusterId)
                .orElseThrow(() -> new IllegalStateException(""))
                .getConnector(cd.getId())
                .getStatus();

            return status != null
                && Objects.equals("provisioning", status.getPhase())
                && getManagedConnectors(cd).get(0).getStatus() != null
                && getManagedConnectors(cd).get(0).getStatus().getResources() != null;
        });

        var resources = new ArrayList<>(getManagedConnectors(cd).get(0).getStatus().getResources());

        fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .updateConnector(cd.getId(), c -> {
                c.getDeployment().getSpec()
                    .setDesiredState(DESIRED_STATE_DELETED);
                c.getDeployment().getMetadata()
                    .setResourceVersion(c.getDeployment().getMetadata().getResourceVersion() + 1);
            });

        await(30, TimeUnit.SECONDS, () -> {
            ConnectorDeploymentStatus status = fm.getCluster(clusterId)
                .orElseThrow(() -> new IllegalStateException(""))
                .getConnector(cd.getId())
                .getStatus();

            return status != null
                && Objects.equals(DESIRED_STATE_DELETED, status.getPhase());
        });

        await(30, TimeUnit.SECONDS, () -> {
            for (var resource : resources) {
                if (uc.getAsNode(namespace, resource) != null) {
                    return false;
                }
            }

            return true;
        });

    }
}
