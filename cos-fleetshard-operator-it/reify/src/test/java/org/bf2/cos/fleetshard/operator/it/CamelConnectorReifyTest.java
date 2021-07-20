package org.bf2.cos.fleetshard.operator.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OidcSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTestResource(OidcSetup.class)
@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorReifyTest extends CamelTestSupport {
    @BeforeEach
    public void setUp() {
        withCamelConnectorOperator("cm-1", "1.1.0");
    }

    @Test
    void managedCamelConnectorIsReified() {
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

    }
}
