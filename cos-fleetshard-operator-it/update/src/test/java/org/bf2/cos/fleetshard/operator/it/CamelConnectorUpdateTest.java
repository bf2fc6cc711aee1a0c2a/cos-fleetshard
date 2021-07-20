package org.bf2.cos.fleetshard.operator.it;

import java.util.Objects;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTest
@TestProfile(CamelTestProfile.class)
public class CamelConnectorUpdateTest extends CamelTestSupport {

    @BeforeEach
    void setUp() {
        withCamelConnectorOperator("cm-1", "1.1.0");
    }

    @Test
    public void managedCamelConnectorUpdate() {
        final ConnectorDeployment cd = withDefaultConnectorDeployment();
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());

        awaitConnector(cd, connector -> {
            assertThat(connector).satisfies(c -> {
                var resources = c.getStatus().getResources();
                var drv = c.getSpec().getDeployment().getDeploymentResourceVersion();

                assertThat(drv).isEqualTo(cd.getMetadata().getResourceVersion());

                assertThat(resources)
                    .hasSize(2)
                    .anyMatch(r -> r.is("v1", "Secret", c.getMetadata().getName() + "-" + drv, drv))
                    .anyMatch(r -> r.is("camel.apache.org/v1alpha1", "KameletBinding", c.getMetadata().getName()));

                assertThat(uc)
                    .hasSecret(namespace, c.getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion())
                    .hasKameletBinding(namespace, c.getMetadata().getName());

            });
        });

        Long currentRevision = mandatoryGetManagedConnector(cd)
            .getSpec()
            .getDeployment()
            .getDeploymentResourceVersion();

        updateConnectorSpec(clusterId, cd.getId(), cs -> {
            cs.with("connector").put("foo", "connector-bar");
        });

        awaitConnector(cd, connector -> {
            assertThat(connector).satisfies(c -> {
                var resources = c.getStatus().getResources();
                var drv = c.getSpec().getDeployment().getDeploymentResourceVersion();

                assertThat(drv).isGreaterThan(currentRevision);

                assertThat(resources)
                    .hasSize(2)
                    .anyMatch(r -> r.is("v1", "Secret", c.getMetadata().getName() + "-" + drv, drv))
                    .anyMatch(r -> r.is("camel.apache.org/v1alpha1", "KameletBinding", c.getMetadata().getName()));

                assertThat(ksrv.getClient().secrets().inNamespace(namespace).list().getItems())
                    .hasSize(1)
                    .first()
                    .matches(s -> Objects.equals(c.getMetadata().getName() + "-" + drv, s.getMetadata().getName()));

                assertThat(uc)
                    .hasSecret(namespace, c.getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion())
                    .hasKameletBinding(namespace, c.getMetadata().getName());
            });
        });
    }
}
