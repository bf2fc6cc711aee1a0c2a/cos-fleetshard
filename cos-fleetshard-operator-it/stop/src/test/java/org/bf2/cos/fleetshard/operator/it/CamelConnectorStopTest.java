package org.bf2.cos.fleetshard.operator.it;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.client.UnstructuredClient;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTestResource(CamelMetaServiceSetup.class)
@QuarkusTest
public class CamelConnectorStopTest extends CamelTestSupport {
    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String camelMeta;
    @ConfigProperty(
        name = "test.namespace")
    String namespace;

    @Test
    void managedCamelConnectorStatusIsReported() throws Exception {
        final UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        final ManagedConnectorOperator op = withConnectorOperator("cm-1", "1.1.0", camelMeta);
        final ConnectorDeployment cd = withConnectorDeployment();

        await(30, TimeUnit.SECONDS, () -> {
            ConnectorDeploymentStatus status = fm.getCluster(clusterId)
                .orElseThrow(() -> new IllegalStateException(""))
                .getConnector(cd.getId())
                .getStatus();

            return status != null
                && Objects.equals("provisioning", status.getPhase())
                && getManagedConnectors(cd).get(0).getStatus() != null
                && getManagedConnectors(cd).get(0).getStatus().getResources() != null;
        });

        var resources = new ArrayList<>(getManagedConnectors(cd).get(0).getStatus().getResources());

        fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .updateConnector(cd.getId(), c -> {
                c.getDeployment().getSpec()
                    .setDesiredState(DESIRED_STATE_STOPPED);
                c.getDeployment().getMetadata()
                    .setResourceVersion(c.getDeployment().getMetadata().getResourceVersion() + 1);
            });

        await(30, TimeUnit.SECONDS, () -> {
            ConnectorDeploymentStatus status = fm.getCluster(clusterId)
                .orElseThrow(() -> new IllegalStateException(""))
                .getConnector(cd.getId())
                .getStatus();

            return status != null
                && Objects.equals(DESIRED_STATE_STOPPED, status.getPhase());
        });

        await(30, TimeUnit.SECONDS, () -> {
            for (var resource : resources) {
                if (uc.getAsNode(namespace, resource) != null) {
                    return false;
                }
            }

            return true;
        });

    }

    private ConnectorDeployment withConnectorDeployment() {
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
}
