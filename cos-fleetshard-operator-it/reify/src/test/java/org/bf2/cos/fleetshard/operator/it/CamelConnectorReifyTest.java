package org.bf2.cos.fleetshard.operator.it;

import java.util.Optional;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OidcSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTestResource(OidcSetup.class)
@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorReifyTest extends CamelTestSupport {
    @Test
    void managedCamelConnectorIsReified() {
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
