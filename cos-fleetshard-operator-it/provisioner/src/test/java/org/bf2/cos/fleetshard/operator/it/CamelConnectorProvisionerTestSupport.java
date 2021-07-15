package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;
import java.util.Optional;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

public class CamelConnectorProvisionerTestSupport extends CamelTestSupport {

    protected void managedCamelConnectorProvisioned() {
        final ManagedConnectorOperator op = withCamelConnectorOperator("cm-1", "1.1.0");
        final ConnectorDeployment cd = withDefaultConnectorDeployment();
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());

        await(() -> {
            Optional<ManagedConnector> connector = getManagedConnector(cd);

            assertThat(connector).get().satisfies(c -> {
                assertThat(uc).hasResource(
                    namespace,
                    "v1",
                    "Secret",
                    c.getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion());
                assertThat(uc).hasResource(
                    namespace,
                    "camel.apache.org/v1alpha1",
                    "KameletBinding",
                    c.getMetadata().getName());
            });
        });
    }
}
