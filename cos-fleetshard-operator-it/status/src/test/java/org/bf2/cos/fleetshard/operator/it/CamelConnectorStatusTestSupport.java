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

public class CamelConnectorStatusTestSupport extends CamelTestSupport {
    protected void managedCamelConnectorStatusIsReported() {
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

        updateKameletBinding(mandatoryGetManagedConnector(cd).getMetadata().getName());

        awaitStatus(clusterId, cd.getId(), status -> {
            assertThat(status.getPhase()).isEqualTo(DESIRED_STATE_READY);
        });
    }

    protected Map<String, Object> updateKameletBinding(String name) {
        return updateUnstructured("camel.apache.org/v1alpha1", "KameletBinding", name, binding -> {
            binding.with("status").put("phase", "Ready");
            binding.with("status").withArray("conditions")
                .addObject()
                .put("message", "a message")
                .put("reason", "a reason")
                .put("status", "the status")
                .put("type", "the type")
                .put("lastTransitionTime", "2021-06-12T12:35:09+02:00");
        });
    }
}
