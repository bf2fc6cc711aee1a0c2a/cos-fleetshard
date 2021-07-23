package org.bf2.cos.fleetshard.operator.it;

import java.util.ArrayList;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OidcSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;
import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTestResource(OidcSetup.class)
@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorStopTest extends CamelTestSupport {

    @BeforeEach
    void setUp() {
        withCamelConnectorOperator("cm-1", "1.1.0");
    }

    @Test
    void managedCamelConnectorStatusIsReported() {
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        final ConnectorDeployment cd = withDefaultConnectorDeployment();

        awaitConnectorDeploymentPhase(clusterId, cd.getId(), STATE_PROVISIONING);
        awaitConnector(cd, connector -> {
            assertThat(connector).hasStatus();
            assertThat(connector).hasResources();
        });

        var resources = new ArrayList<>(mandatoryGetManagedConnector(cd).getStatus().getResources());

        setConnectorDeploymentDesiredState(clusterId, cd.getId(), DESIRED_STATE_STOPPED);
        awaitConnectorDeploymentPhase(clusterId, cd.getId(), STATE_STOPPED);

        await(() -> {
            assertThat(resources).noneMatch(r -> uc.getAsNode(namespace, r) != null);
        });

        setConnectorDeploymentDesiredState(clusterId, cd.getId(), DESIRED_STATE_READY);
        awaitConnectorDeploymentPhase(clusterId, cd.getId(), STATE_PROVISIONING);

        awaitConnector(cd, connector -> {
            assertThat(connector).hasStatus();
            assertThat(connector).hasResources();
        });
    }
}
