package org.bf2.cos.fleetshard.operator.it;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.KafkaConnectionSettings;
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
    void managedCamelConnectorStatusIsReported() throws Exception {
        final ManagedConnectorOperator op = withConnectorOperator("cm-1", "1.1.0", camelMeta);
        final ConnectorDeployment cd = withConnectorDeployment();
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());

        await(30, TimeUnit.SECONDS, () -> {
            List<ManagedConnector> connectors = getManagedConnectors(cd);
            if (connectors.size() != 1) {
                return false;
            }

            JsonNode secret = uc.getAsNode(
                namespace,
                "v1",
                "Secret",
                connectors.get(0).getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion());

            JsonNode binding = uc.getAsNode(
                namespace,
                "camel.apache.org/v1alpha1",
                "KameletBinding",
                connectors.get(0).getMetadata().getName());

            return secret != null && binding != null;
        });

        updateKameletBinding(getManagedConnectors(cd).get(0).getMetadata().getName());

        await(30, TimeUnit.SECONDS, () -> {
            ConnectorDeploymentStatus status = fm.getCluster(clusterId)
                .orElseThrow(() -> new IllegalStateException(""))
                .getConnector(cd.getId())
                .getStatus();

            // TODO: unstructured update event seems not propagated
            return status != null
                && Objects.equals(DESIRED_STATE_READY, status.getPhase());
        });

    }

    private ConnectorDeployment withConnectorDeployment() {
        final String barB64 = Base64.getEncoder()
            .encodeToString("bar".getBytes(StandardCharsets.UTF_8));
        final String kcidB64 = Base64.getEncoder()
            .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        final String deploymentId = UUID.randomUUID().toString();
        final String connectorId = "cid";
        final String connectorTypeId = "ctid";

        final ObjectNode connectorSpec = Serialization.jsonMapper().createObjectNode();
        connectorSpec.with("connector").put("foo", "connector-foo");
        connectorSpec.with("connector").with("bar").put("kind", "base64").put("value", barB64);
        connectorSpec.with("kafka").put("topic", "kafka-foo");
        connectorSpec.withArray("steps").addObject().with("extract-field").put("field", "field");
        connectorSpec.withArray("steps").addObject().with("insert-field").put("field", "a-field").put("value", "a-value");
        connectorSpec.withArray("steps").addObject().with("insert-field").put("field", "b-field").put("value", "b-value");

        final ObjectNode connectorMeta = Serialization.jsonMapper().createObjectNode();
        connectorMeta.put("connector_type", "sink");
        connectorMeta.put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.0.1");
        connectorMeta.withArray("operators").addObject()
            .put("type", "camel-connector-operator")
            .put("version", "[1.0.0,2.0.0)");
        connectorMeta.with("kamelets")
            .put("connector", "aws-s3-sink")
            .put("kafka", "managed-kafka-source")
            .put("insert-field", "insert-field-action")
            .put("extract-field", "extract-field-action");

        ConnectorDeployment cd = new ConnectorDeployment()
            .kind("ConnectorDeployment")
            .id(deploymentId)
            .metadata(new ConnectorDeploymentAllOfMetadata()
                .resourceVersion(2L))
            .spec(new ConnectorDeploymentSpec()
                .connectorId(connectorId)
                .connectorTypeId(connectorTypeId)
                .connectorResourceVersion(1L)
                .kafka(new KafkaConnectionSettings()
                    .bootstrapServer("kafka.acme.com:2181")
                    .clientId(UUID.randomUUID().toString())
                    .clientSecret(kcidB64))
                .connectorSpec(connectorSpec)
                .shardMetadata(connectorMeta)
                .desiredState(DESIRED_STATE_READY));

        return fm.getOrCreatCluster(clusterId).setConnectorDeployment(cd);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> updateKameletBinding(String name) throws Exception {
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

        return ksrv.getClient()
            .customResource(asCustomResourceDefinitionContext(binding))
            .updateStatus(
                namespace,
                name,
                Serialization.jsonMapper().treeToValue(binding, Map.class));
    }
}
