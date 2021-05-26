package org.bf2.cos.fleetshard.operator.it;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.api.model.cp.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpec;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.it.support.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.CamelTestSupport;
import org.bf2.cos.fleetshard.operator.it.support.FleetManager;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.bf2.cos.fleetshard.operator.support.UnstructuredClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@TestProfile(CamelConnectorReifyTest.Profile.class)
@QuarkusTest
public class CamelConnectorReifyTest extends CamelTestSupport {
    @KubernetesTestServer
    KubernetesServer ksrv;
    @Inject
    FleetManager fm;

    @ConfigProperty(
        name = "cluster-id")
    String clusterId;

    @ConfigProperty(
        name = "cos.connectors.namespace")
    String connectorsNamespace;

    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    String connectorsMeta;

    @Test
    void managedCamelConnectorIsReified() {
        var mco = new ManagedConnectorOperator();
        mco.setMetadata(new ObjectMeta());
        mco.getMetadata().setName("cm-1");
        mco.getMetadata().setNamespace("test");
        mco.setSpec(new ManagedConnectorOperatorSpec());
        mco.getSpec().setNamespace("test");
        mco.getSpec().setType("camel-connector-operator");
        mco.getSpec().setType("camel-connector-operator");
        mco.getSpec().setVersion("1.1.0");
        mco.getSpec().setMetaService(connectorsMeta);

        ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(connectorsNamespace)
            .createOrReplace(mco);

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
                .desiredState("ready"));

        fm.getOrCreatCluster(clusterId).setConnectorDeployment(cd);

        TestSupport.await(
            30,
            TimeUnit.SECONDS,
            () -> {
                var result = ksrv.getClient()
                    .customResources(ManagedConnector.class)
                    .inNamespace(connectorsNamespace)
                    .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, connectorId)
                    .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deploymentId)
                    .list();

                return result.getItems() != null && result.getItems().size() == 1;
            });

        TestSupport.await(
            30,
            TimeUnit.SECONDS,
            () -> {
                var connector = ksrv.getClient()
                    .customResources(ManagedConnector.class)
                    .inNamespace(connectorsNamespace)
                    .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, connectorId)
                    .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deploymentId)
                    .list()
                    .getItems()
                    .get(0);

                var secret = new UnstructuredClient(ksrv.getClient()).getAsNode(
                    connectorsNamespace,
                    new ResourceRef(
                        "v1",
                        "Secret",
                        connector.getMetadata().getName() + "-" + cd.getMetadata().getResourceVersion()));

                var binding = new UnstructuredClient(ksrv.getClient()).getAsNode(
                    connectorsNamespace,
                    new ResourceRef(
                        "camel.apache.org/v1alpha1",
                        "KameletBinding",
                        connector.getMetadata().getName()));

                return secret != null && binding != null;
            });
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(OperatorSetup.class),
                new TestResourceEntry(KubernetesSetup.class),
                new TestResourceEntry(CamelMetaServiceSetup.class));
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
