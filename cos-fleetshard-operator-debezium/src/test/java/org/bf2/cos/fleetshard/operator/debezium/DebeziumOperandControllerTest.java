package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ServiceAccountSpecBuilder;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.model.AbstractApicurioConverter;
import org.bf2.cos.fleetshard.operator.debezium.model.ApicurioAvroConverter;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectJsonConverter;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;
import org.bf2.cos.fleetshard.operator.debezium.model.KeyAndValueConverters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.JmxPrometheusExporterMetrics;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaConnectStatusBuilder;
import io.strimzi.api.kafka.model.status.KafkaConnectorStatusBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DebeziumOperandControllerTest {
    private static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid";
    private static final Long DEFAULT_CONNECTOR_REVISION = 1L;
    private static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid";
    private static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/pg:1";
    private static final String DEFAULT_DEPLOYMENT_ID = "1";
    private static final Long DEFAULT_DEPLOYMENT_REVISION = 1L;
    private static final String CLIENT_ID = "kcid";
    private static final String CLIENT_SECRET = Base64.getEncoder().encodeToString("kcs".getBytes(StandardCharsets.UTF_8));
    private static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181";
    private static final String PG_CLASS = "io.debezium.connector.postgresql.PostgresConnector";
    private static final String SCHEMA_REGISTRY_URL = "https://bu98.serviceregistry.rhcloud.com/t/51eba005-daft-punk-afe1-b2178bcb523d/apis/registry/v2";
    private static final String SCHEMA_REGISTRY_ID = "9bsv0s0k8lng031se9q0";
    private static final String MANAGED_CONNECTOR_UID = "51eba005-daft-punk-afe1-b2178bcb523d";
    private static final String IMAGE_PULL_SECRET_NAME = "my-pullsecret";

    private static final DebeziumOperandConfiguration CONFIGURATION = new DebeziumOperandConfiguration() {
        @Override
        public LocalObjectReference imagePullSecretsName() {
            return new LocalObjectReference(IMAGE_PULL_SECRET_NAME);
        }

        @Override
        public KafkaConnect kafkaConnect() {
            return Map::of;
        }

        @Override
        public KafkaConnector kafkaConnector() {
            return Map::of;
        }
    };

    @Test
    void declaresExpectedResourceTypes() {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(kubernetesClient, CONFIGURATION);

        assertThat(controller.getResourceTypes())
            .hasSize(2)
            .anyMatch(ctx -> Constants.RESOURCE_GROUP_NAME.equals(ctx.getGroup())
                && KafkaConnect.CONSUMED_VERSION.equals(ctx.getVersion())
                && KafkaConnect.RESOURCE_KIND.equals(ctx.getKind()))
            .anyMatch(ctx -> Constants.RESOURCE_GROUP_NAME.equals(ctx.getGroup())
                && KafkaConnector.CONSUMED_VERSION.equals(ctx.getVersion())
                && KafkaConnector.RESOURCE_KIND.equals(ctx.getKind()));
    }

    private ObjectNode getSpec() {
        var spec = Serialization.jsonMapper().createObjectNode()
            .put("database.hostname", "orderdb")
            .put("database.port", "5432")
            .put("database.user", "orderuser")
            .put("database.dbname", "orderdb")
            .put("database.server.name", "dbserver1")
            .put("schema.include.list", "purchaseorder")
            .put("table.include.list", "purchaseorder.outboxevent")
            .put("tombstones.on.delete", "false")
            .put("transforms", "saga")
            .put("transforms.saga.type", "io.debezium.transforms.outbox.EventRouter")
            .put("transforms.saga.route.topic.replacement", "${routedByValue}.request")
            .put("poll.interval.ms", "100")
            .put("consumer.interceptor.classes", "io.opentracing.contrib.kafka.TracingConsumerInterceptor")
            .put("producer.interceptor.classes", "io.opentracing.contrib.kafka.TracingProducerInterceptor");
        var pwdB64 = Base64.getEncoder().encodeToString("orderpw".getBytes(StandardCharsets.UTF_8));
        spec.with("database.password").put("kind", "base64").put("value", pwdB64);
        return spec;
    }

    private ObjectNode addAvroToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.with("data_shape").put("key", "AVRO").put("value", "AVRO");
        return baseConfig;
    }

    private ObjectNode addJsonWithSchemaToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.with("data_shape").put("key", "JSON").put("value", "JSON");
        return baseConfig;
    }

    private ObjectNode addSchemalessJsonToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.with("data_shape").put("key", "JSON without schema").put("value", "JSON without schema");
        return baseConfig;
    }

    void reify(ObjectNode connectorConfig, Consumer<KafkaConnect> kafkaConnectChecks) {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(kubernetesClient, CONFIGURATION);

        var resources = controller.doReify(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withUid(MANAGED_CONNECTOR_UID)
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withConnectorId(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withDeploymentId(DEFAULT_DEPLOYMENT_ID)
                    .withDeployment(new DeploymentSpecBuilder()
                        .withConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID)
                        .withSecret("secret")
                        .withKafka(new KafkaSpecBuilder().withUrl(DEFAULT_KAFKA_SERVER).build())
                        .withNewSchemaRegistry(SCHEMA_REGISTRY_ID, SCHEMA_REGISTRY_URL)
                        .withConnectorResourceVersion(DEFAULT_CONNECTOR_REVISION)
                        .withDeploymentResourceVersion(DEFAULT_DEPLOYMENT_REVISION)
                        .withDesiredState(DESIRED_STATE_READY)
                        .build())
                    .build())
                .build(),
            new org.bf2.cos.fleetshard.operator.debezium.DebeziumShardMetadataBuilder()
                .withContainerImage(DEFAULT_CONNECTOR_IMAGE)
                .withConnectorClass(PG_CLASS)
                .build(),
            new ConnectorConfiguration<>(connectorConfig, ObjectNode.class,
                DebeziumDataShape.class),
            new ServiceAccountSpecBuilder()
                .withClientId(CLIENT_ID)
                .withClientSecret(CLIENT_SECRET)
                .build());

        assertThat(resources)
            .anyMatch(DebeziumOperandSupport::isKafkaConnect)
            .anyMatch(DebeziumOperandSupport::isKafkaConnector)
            .anyMatch(DebeziumOperandSupport::isSecret)
            .anyMatch(DebeziumOperandSupport::isConfigMap);

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnect)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnect.class, kc -> {
                assertThat(kc.getSpec().getImage()).isEqualTo(DEFAULT_CONNECTOR_IMAGE);
                assertThat(kc.getSpec().getTemplate().getPod().getImagePullSecrets())
                    .contains(CONFIGURATION.imagePullSecretsName());
                assertThat(kc.getSpec().getMetricsConfig().getType()).isEqualTo("jmxPrometheusExporter");
                assertThat(kc.getSpec().getMetricsConfig()).isInstanceOfSatisfying(JmxPrometheusExporterMetrics.class,
                    jmxMetricsConfig -> {
                        assertThat(jmxMetricsConfig.getValueFrom().getConfigMapKeyRef().getKey())
                            .isEqualTo(DebeziumOperandController.METRICS_CONFIG_FILENAME);
                        assertThat(jmxMetricsConfig.getValueFrom().getConfigMapKeyRef().getName())
                            .isEqualTo(
                                DEFAULT_MANAGED_CONNECTOR_ID
                                    + DebeziumOperandController.KAFKA_CONNECT_METRICS_CONFIGMAP_NAME_SUFFIX);
                    });
            });

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isConfigMap)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(ConfigMap.class, configMap -> {
                assertThat(configMap.getData())
                    .containsKey(DebeziumOperandController.METRICS_CONFIG_FILENAME);
                assertThat(configMap.getData().get(DebeziumOperandController.METRICS_CONFIG_FILENAME))
                    .isEqualTo(DebeziumOperandController.METRICS_CONFIG);
            });

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnector)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnector.class, kctr -> {
                assertThat(
                    kctr.getSpec().getConfig()).containsEntry(
                        "database.password",
                        "${file:/opt/kafka/external-configuration/"
                            + DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY
                            + "/"
                            + EXTERNAL_CONFIG_FILE
                            + ":database.password}");
                assertThat(kctr.getSpec().getConfig().get(DebeziumOperandController.CONFIG_OPTION_POSTGRES_PLUGIN_NAME))
                    .isEqualTo(DebeziumOperandController.PLUGIN_NAME_PGOUTPUT);
            });

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnect)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnect.class, kafkaConnectChecks);
    }

    @Test
    void testReifyWithSchemalessJson() {
        this.reify(addSchemalessJsonToConnectorConfig(getSpec()),
            kafkaConnect -> {
                assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                    KafkaConnectJsonConverter.CONVERTER_CLASS);
                assertThat(kafkaConnect.getSpec().getConfig())
                    .containsEntry(KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable", "false");
                assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                    KafkaConnectJsonConverter.CONVERTER_CLASS);
                assertThat(kafkaConnect.getSpec().getConfig())
                    .containsEntry(KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable", "false");
            });
    }

    private Consumer<KafkaConnect> getApicurioChecks(String converterClass) {
        return kafkaConnect -> {
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                converterClass);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                converterClass);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.service.url",
                AbstractApicurioConverter.APICURIO_AUTH_SERVICE_URL);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.service.url",
                AbstractApicurioConverter.APICURIO_AUTH_SERVICE_URL);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.realm", "rhoas");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.realm", "rhoas");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.url",
                SCHEMA_REGISTRY_URL);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.url",
                SCHEMA_REGISTRY_URL);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.client.id",
                CLIENT_ID);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.client.id",
                CLIENT_ID);
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.client.secret",
                "${dir:/opt/kafka/external-configuration/connector-configuration:_kafka.client.secret}");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.client.secret",
                "${dir:/opt/kafka/external-configuration/connector-configuration:_kafka.client.secret}");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.auto-register", "true");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.auto-register", "true");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.find-latest", "true");
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.find-latest", "true");
        };
    }

    @Test
    void testReifyWithAvro() {
        this.reify(addAvroToConnectorConfig(getSpec()), getApicurioChecks(ApicurioAvroConverter.CONVERTER_CLASS));
    }

    @Test
    void testReifyWithJsonWithSchema() {
        this.reify(addJsonWithSchemaToConnectorConfig(getSpec()),
            kafkaConnect -> {
                assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                    KafkaConnectJsonConverter.CONVERTER_CLASS);
                assertThat(kafkaConnect.getSpec().getConfig())
                    .containsEntry(KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable", "true");
                assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                    KafkaConnectJsonConverter.CONVERTER_CLASS);
                assertThat(kafkaConnect.getSpec().getConfig())
                    .containsEntry(KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable", "true");
            });
    }

    private static Condition createCondition(String type, String status, String reason) {
        return new ConditionBuilder().withType(type).withStatus(status).withReason(reason).build();
    }

    private static List<Condition> createConditions(
        String readyConditionStatus, String readyConditionReason,
        String notReadyConditionReason) {
        return List.of(
            createCondition("Ready", readyConditionStatus, readyConditionReason),
            createCondition("NotReady", "True", notReadyConditionReason));
    }

    private static List<Condition> createConditions(
        String readyConditionStatus, String readyConditionReason) {
        return List.of(createCondition("Ready", readyConditionStatus, readyConditionReason));
    }

    public static Stream<Arguments> computeStatus() {
        return Stream.of(
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("True", null),
                createConditions("True", null),
                ManagedConnector.STATE_READY,
                null),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("True", null),
                createConditions("False", "reason", "TimeoutException"),
                ManagedConnector.STATE_FAILED,
                "KafkaClusterUnreachable"),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("False", "reason", "reason"),
                createConditions("True", null),
                ManagedConnector.STATE_PROVISIONING,
                "reason"),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("False", "reason", "ConnectRestException"),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                "ConnectRestException"),
            arguments(
                KafkaConnectorStatus.STATE_FAILED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                null),
            arguments(
                KafkaConnectorStatus.STATE_PAUSED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_STOPPED,
                null),
            arguments(
                KafkaConnectorStatus.STATE_UNASSIGNED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_PROVISIONING,
                null));
    }

    @ParameterizedTest
    @MethodSource
    void computeStatus(
        String connectorState,
        List<Condition> connectorConditions,
        List<Condition> connectConditions,
        String expectedManagedConnectorState,
        String expectedReason) {

        ConnectorStatusSpec status = new ConnectorStatusSpec();

        DebeziumOperandSupport.computeStatus(
            status,
            new KafkaConnectBuilder()
                .withStatus(new KafkaConnectStatusBuilder()
                    .addAllToConditions(connectConditions)
                    .build())
                .build(),
            new KafkaConnectorBuilder()
                .withStatus(new KafkaConnectorStatusBuilder()
                    .addAllToConditions(connectorConditions)
                    .addToConnectorStatus("connector",
                        new org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatusBuilder()
                            .withState(connectorState)
                            .build())
                    .build())
                .build());

        assertThat(status.getPhase()).isEqualTo(expectedManagedConnectorState);
        if ("KafkaClusterUnreachable".equals(expectedReason)) {
            assertThat(status.getConditions()).anySatisfy(condition -> assertThat(condition)
                .hasFieldOrPropertyWithValue("type", "KafkaConnect:NotReady")
                .hasFieldOrPropertyWithValue("status", "True")
                .hasFieldOrPropertyWithValue("reason", expectedReason));
        } else if ("ConnectRestException".equals(expectedReason)) {
            assertThat(status.getConditions()).anySatisfy(condition -> assertThat(condition)
                .hasFieldOrPropertyWithValue("type", "KafkaConnector:NotReady")
                .hasFieldOrPropertyWithValue("status", "True")
                .hasFieldOrPropertyWithValue("reason", expectedReason));
        } else {
            assertThat(status.getConditions()).anySatisfy(condition -> assertThat(condition)
                .hasFieldOrPropertyWithValue("status", null == expectedReason ? "True" : "False")
                .hasFieldOrPropertyWithValue("reason", expectedReason));
            assertThat(status.getConditions()).anySatisfy(condition -> assertThat(condition)
                .hasFieldOrPropertyWithValue("type", "KafkaConnect:Ready")
                .hasFieldOrPropertyWithValue("status", "True")
                .hasFieldOrPropertyWithValue("reason", null));
        }
    }
}
