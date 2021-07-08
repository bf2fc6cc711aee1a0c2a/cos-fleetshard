package org.bf2.cos.fleetshard.operator.it;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
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

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorReifyTest extends CamelTestSupport {
    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;
    @ConfigProperty(
        name = "test.namespace")
    String namespace;

    @Test
    void managedCamelConnectorIsReified() {
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

    }
}
