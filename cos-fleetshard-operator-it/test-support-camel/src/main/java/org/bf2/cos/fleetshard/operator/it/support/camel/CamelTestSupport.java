package org.bf2.cos.fleetshard.operator.it.support.camel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.support.ResourceUtil.asCustomResourceDefinitionContext;

public class CamelTestSupport extends TestSupport {
    static {
        KubernetesDeserializer.registerCustomKind("camel.apache.org/v1alpha1", "KameletBinding", KameletBindingResource.class);
    }

    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;

    protected ManagedConnectorOperator withCamelConnectorOperator(String name, String version) {
        return withConnectorOperator(name, "camel-connector-operator", version, camelMeta);
    }

    protected ConnectorDeployment withDefaultConnectorDeployment() {
        final String kcidB64 = Base64.getEncoder()
            .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        final String deploymentId = UUID.randomUUID().toString();
        final String connectorId = "cid";
        final String connectorTypeId = "ctid";

        final ObjectNode connectorSpec = Serialization.jsonMapper().createObjectNode();
        connectorSpec.with("connector").put("foo", "connector-foo");
        connectorSpec.with("kafka").put("topic", "kafka-foo");

        final ObjectNode connectorMeta = Serialization.jsonMapper().createObjectNode();
        connectorMeta.put("connector_type", "sink");
        connectorMeta.put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.0.1");
        connectorMeta.withArray("operators").addObject()
            .put("type", "camel-connector-operator")
            .put("version", "[1.0.0,2.0.0)");
        connectorMeta.with("kamelets")
            .put("connector", "aws-s3-sink")
            .put("kafka", "managed-kafka-source");

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

    @JsonDeserialize
    public static class KameletBindingResource extends HashMap<String, Object> implements KubernetesResource {
    }
}
