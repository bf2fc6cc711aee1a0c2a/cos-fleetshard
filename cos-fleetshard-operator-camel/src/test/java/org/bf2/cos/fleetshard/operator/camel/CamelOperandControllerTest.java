package org.bf2.cos.fleetshard.operator.camel;

import java.util.Base64;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatusBuilder;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_FAILED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_READY;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS;

public final class CamelOperandControllerTest {
    private static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid";
    private static final Long DEFAULT_CONNECTOR_REVISION = 1L;
    private static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid";
    private static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/s3:1";
    private static final String DEFAULT_DEPLOYMENT_ID = "1";
    private static final Long DEFAULT_DEPLOYMENT_REVISION = 1L;
    private static final String DEFAULT_KAFKA_CLIENT_ID = "kcid";
    private static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181";

    @Test
    void declaresExpectedResourceTypes() {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        CamelOperandConfiguration configuration = Mockito.mock(CamelOperandConfiguration.class);
        CamelOperandController controller = new CamelOperandController(kubernetesClient, configuration);

        assertThat(controller.getResourceTypes())
            .hasSize(1)
            .first()
            .matches(ctx -> {
                return KameletBinding.RESOURCE_KIND.equals(ctx.getKind())
                    && KameletBinding.RESOURCE_GROUP.equals(ctx.getGroup())
                    && KameletBinding.RESOURCE_VERSION.equals(ctx.getVersion());
            });
    }

    @Test
    void reify() {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        CamelOperandConfiguration configuration = Mockito.mock(CamelOperandConfiguration.class);
        CamelOperandController controller = new CamelOperandController(kubernetesClient, configuration);

        final String barB64 = Secrets.toBase64("bar");
        final String kcsB64 = Secrets.toBase64("kcs");

        final ObjectNode spec = Serialization.jsonMapper().createObjectNode();
        spec.with("kafka").put("topic", "kafka-foo");
        spec.with("connector").with("bar").put("kind", "base64").put("value", barB64);
        spec.with("connector").put("foo", "connector-foo");
        spec.withArray("steps").addObject().with("extract-field").put("field", "field");
        spec.withArray("steps").addObject().with("insert-field").put("field", "a-field").put("value", "a-value");
        spec.withArray("steps").addObject().with("insert-field").put("field", "b-field").put("value", "b-value");

        var resources = controller.doReify(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(DEFAULT_MANAGED_CONNECTOR_ID)
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withConnectorId(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withDeploymentId(DEFAULT_DEPLOYMENT_ID)
                    .withDeployment(new DeploymentSpecBuilder()
                        .withConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID)
                        .withSecret("secret")
                        .withConnectorResourceVersion(DEFAULT_CONNECTOR_REVISION)
                        .withDeploymentResourceVersion(DEFAULT_DEPLOYMENT_REVISION)
                        .withDesiredState(DESIRED_STATE_READY)
                        .build())
                    .build())
                .build(),
            new CamelShardMetadataBuilder()
                .withConnectorImage(DEFAULT_CONNECTOR_IMAGE)
                .withConnectorRevision("" + DEFAULT_CONNECTOR_REVISION)
                .withConnectorType(CONNECTOR_TYPE_SOURCE)
                .addToKamelets("connector", "aws-kinesis-source")
                .addToKamelets("kafka", "managed-kafka-sink")
                .addToKamelets("insert-field", "insert-field-action")
                .addToKamelets("extract-field", "extract-field-action")
                .build(),
            spec,
            new KafkaSpecBuilder()
                .withBootstrapServers(DEFAULT_KAFKA_SERVER)
                .withClientId(DEFAULT_KAFKA_CLIENT_ID)
                .withClientSecret(kcsB64)
                .build());

        assertThat(resources)
            .anySatisfy(resource -> {
                assertThat(resource.getApiVersion()).isEqualTo(KameletBinding.RESOURCE_API_VERSION);
                assertThat(resource.getKind()).isEqualTo(KameletBinding.RESOURCE_KIND);

                assertThat(resource.getMetadata().getAnnotations())
                    .containsEntry(TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE, DEFAULT_CONNECTOR_IMAGE)
                    .containsEntry(TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED, "false")
                    .containsEntry(TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED, "false")
                    .containsEntry(TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON, "false")
                    .containsEntry(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS, LABELS_TO_TRANSFER);

                /*
                 * assertThat(annotation(node, format(TRAIT_CAMEL_APACHE_ORG_ENV, "COS_MANAGED_CONNECTOR_ID")).asText())
                 * .isEqualTo(DEFAULT_MANAGED_CONNECTOR_ID);
                 * assertThat(annotation(node, format(TRAIT_CAMEL_APACHE_ORG_ENV, "COS_DEPLOYMENT_ID")).asText())
                 * .isEqualTo(DEFAULT_DEPLOYMENT_ID);
                 * assertThat(annotation(node, format(TRAIT_CAMEL_APACHE_ORG_ENV, "COS_DEPLOYMENT_REVISION")).asLong())
                 * .isEqualTo(DEFAULT_DEPLOYMENT_REVISION);
                 * assertThat(annotation(node, format(TRAIT_CAMEL_APACHE_ORG_ENV, "COS_CONNECTOR_ID")).asText())
                 * .isEqualTo(DEFAULT_CONNECTOR_ID);
                 * assertThat(annotation(node, format(TRAIT_CAMEL_APACHE_ORG_ENV, "COS_CONNECTOR_REVISION")).asLong())
                 * .isEqualTo(DEFAULT_CONNECTOR_REVISION);
                 */

                assertThat(resource).isInstanceOfSatisfying(KameletBinding.class, binding -> {
                    assertThat(binding.getSpec().getSource().getRef())
                        .hasFieldOrPropertyWithValue("apiVersion", Kamelet.RESOURCE_API_VERSION)
                        .hasFieldOrPropertyWithValue("kind", Kamelet.RESOURCE_KIND)
                        .hasFieldOrPropertyWithValue("name", "aws-kinesis-source");
                    assertThat(binding.getSpec().getSink().getRef())
                        .hasFieldOrPropertyWithValue("apiVersion", Kamelet.RESOURCE_API_VERSION)
                        .hasFieldOrPropertyWithValue("kind", Kamelet.RESOURCE_KIND)
                        .hasFieldOrPropertyWithValue("name", "managed-kafka-sink");

                    assertThat(binding.getSpec().getSteps()).element(0).satisfies(step -> {
                        assertThat(step.getRef())
                            .hasFieldOrPropertyWithValue("apiVersion", Kamelet.RESOURCE_API_VERSION)
                            .hasFieldOrPropertyWithValue("kind", Kamelet.RESOURCE_KIND)
                            .hasFieldOrPropertyWithValue("name", "extract-field-action");
                        assertThat(step.getProperties())
                            .containsEntry("id", "extract-field-action-0");
                    });
                    assertThat(binding.getSpec().getSteps()).element(1).satisfies(step -> {
                        assertThat(step.getRef())
                            .hasFieldOrPropertyWithValue("apiVersion", Kamelet.RESOURCE_API_VERSION)
                            .hasFieldOrPropertyWithValue("kind", Kamelet.RESOURCE_KIND)
                            .hasFieldOrPropertyWithValue("name", "insert-field-action");
                        assertThat(step.getProperties())
                            .containsEntry("id", "insert-field-action-1");
                    });
                    assertThat(binding.getSpec().getSteps()).element(2).satisfies(step -> {
                        assertThat(step.getRef())
                            .hasFieldOrPropertyWithValue("apiVersion", Kamelet.RESOURCE_API_VERSION)
                            .hasFieldOrPropertyWithValue("kind", Kamelet.RESOURCE_KIND)
                            .hasFieldOrPropertyWithValue("name", "insert-field-action");
                        assertThat(step.getProperties())
                            .containsEntry("id", "insert-field-action-2");
                    });
                });
            });

        assertThat(resources)
            .anySatisfy(resource -> {
                assertThat(resource.getApiVersion()).isEqualTo("v1");
                assertThat(resource.getKind()).isEqualTo("Secret");

                assertThat(resource.getMetadata().getAnnotations());

                Secret secret = Serialization.jsonMapper().convertValue(resource, Secret.class);
                String encoded = secret.getData().get("application.properties");
                byte[] decoded = Base64.getDecoder().decode(encoded);

                assertThat(new String(decoded))
                    .contains("camel.kamelet.managed-kafka-sink.topic=kafka-foo")
                    .contains("camel.kamelet.managed-kafka-sink.bootstrapServers=kafka.acme.com:2181")
                    .contains("camel.kamelet.managed-kafka-sink.user=kcid")
                    .contains("camel.kamelet.managed-kafka-sink.password=kcs")
                    .contains("camel.kamelet.aws-kinesis-source.bar=bar")
                    .contains("camel.kamelet.aws-kinesis-source.foo=connector-foo")
                    .contains("camel.kamelet.extract-field-action.extract-field-action-0.field=field")
                    .contains("camel.kamelet.insert-field-action.insert-field-action-1.field=a-field")
                    .contains("camel.kamelet.insert-field-action.insert-field-action-1.value=a-value")
                    .contains("camel.kamelet.insert-field-action.insert-field-action-2.field=b-field")
                    .contains("camel.kamelet.insert-field-action.insert-field-action-2.value=b-value");
            });
    }

    @Test
    void status() {
        {
            ConnectorStatusSpec status = new ConnectorStatusSpec();

            CamelOperandSupport.computeStatus(
                status,
                new KameletBindingStatusBuilder()
                    .withPhase(KameletBindingStatus.PHASE_READY)
                    .addToConditions(new ConditionBuilder()
                        .withType("Ready")
                        .withStatus("False")
                        .withReason("reason")
                        .withMessage("message")
                        .build())
                    .build());

            assertThat(status.getPhase()).isEqualTo(STATE_READY);
            assertThat(status.getConditions()).anySatisfy(condition -> {
                assertThat(condition)
                    .hasFieldOrPropertyWithValue("type", "Ready")
                    .hasFieldOrPropertyWithValue("reason", "reason");
            });
        }

        {
            ConnectorStatusSpec status = new ConnectorStatusSpec();

            CamelOperandSupport.computeStatus(
                status,
                new KameletBindingStatusBuilder()
                    .withPhase(KameletBindingStatus.PHASE_ERROR)
                    .addToConditions(new ConditionBuilder()
                        .withType("Ready")
                        .withStatus("False")
                        .withReason("reason")
                        .withMessage("message")
                        .build())
                    .build());

            assertThat(status.getPhase()).isEqualTo(STATE_FAILED);
            assertThat(status.getConditions()).anySatisfy(condition -> {
                assertThat(condition)
                    .hasFieldOrPropertyWithValue("type", "Ready")
                    .hasFieldOrPropertyWithValue("reason", "reason");
            });
        }
    }
}
