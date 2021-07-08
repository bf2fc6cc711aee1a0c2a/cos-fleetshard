package org.bf2.cos.fleetshard.operator.it;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
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
import static org.bf2.cos.fleetshard.operator.support.ResourceUtil.asCustomResourceDefinitionContext;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorStatusTest extends CamelTestSupport {
    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;
    @ConfigProperty(
        name = "test.namespace")
    String namespace;

    @Test
    void managedCamelConnectorStatusIsReported() {
        final ManagedConnectorOperator op = withConnectorOperator("cm-1", "1.1.0", camelMeta);
        final ConnectorDeployment cd = withDefaultConnectorDeployment();
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());

        await(() -> {
            Optional<ManagedConnector> connector = getManagedConnector(cd);
            if (connector.isEmpty()) {
                return false;
            }

            JsonNode secret = uc.getAsNode(
                namespace,
                "v1",
                "Secret",
                connector.get().getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion());

            JsonNode binding = uc.getAsNode(
                namespace,
                "camel.apache.org/v1alpha1",
                "KameletBinding",
                connector.get().getMetadata().getName());

            return secret != null && binding != null;
        });

        updateKameletBinding(mandatoryGetManagedConnector(cd).getMetadata().getName());

        awaitStatus(clusterId, cd.getId(), status -> {
            return Objects.equals(DESIRED_STATE_READY, status.getPhase());
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> updateKameletBinding(String name) {
        UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        ObjectNode binding = (ObjectNode) uc.getAsNode(
            namespace,
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            name);

        binding.with("status").put("phase", "Ready");
        binding.with("status").withArray("conditions")
            .addObject()
            .put("message", "a message")
            .put("reason", "a reason")
            .put("status", "the status")
            .put("type", "the type")
            .put("lastTransitionTime", "2021-06-12T12:35:09+02:00");

        try {
            return ksrv.getClient()
                .customResource(asCustomResourceDefinitionContext(binding))
                .updateStatus(
                    namespace,
                    name,
                    Serialization.jsonMapper().treeToValue(binding, Map.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
