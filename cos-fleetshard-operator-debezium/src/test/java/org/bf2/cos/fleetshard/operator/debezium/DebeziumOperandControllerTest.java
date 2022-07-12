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
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_CLIENT_SECRET_KEY;
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
    private static final String MYSQL_CLASS = "io.debezium.connector.mysql.MysqlConnector";
    private static final String SCHEMA_REGISTRY_URL = "https://bu98.serviceregistry.rhcloud.com/t/51eba005-daft-punk-afe1-b2178bcb523d/apis/registry/v2";
    private static final String SCHEMA_REGISTRY_ID = "9bsv0s0k8lng031se9q0";
    private static final String MANAGED_CONNECTOR_UID = "51eba005-daft-punk-afe1-b2178bcb523d";
    private static final String IMAGE_PULL_SECRET_NAME = "my-pullsecret";
    private static final String APICURIO_AUTH_SERVICE_URL = "https://identity.api.openshift.com/myauth";
    private static final String APICURIO_AUTH_REALM = "my-rhoas";

    private static final DebeziumOperandConfiguration CONFIGURATION = new DebeziumOperandConfiguration() {
        @Override
        public LocalObjectReference imagePullSecretsName() {
            return new LocalObjectReference(IMAGE_PULL_SECRET_NAME);
        }

        @Override
        public String apicurioAuthServiceUrl() {
            return APICURIO_AUTH_SERVICE_URL;
        }

        @Override
        public String apicurioAuthRealm() {
            return APICURIO_AUTH_REALM;
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

    void reify(String connectorClass, ObjectNode connectorConfig, Consumer<KafkaConnect> kafkaConnectChecks) {
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
                .withConnectorClass(connectorClass)
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

                if (PG_CLASS.equals(connectorClass)) {
                    // Specifically test the plugin name for PostgreSQL
                    assertThat(kctr.getSpec().getConfig().get(DebeziumOperandController.CONFIG_OPTION_POSTGRES_PLUGIN_NAME))
                        .isEqualTo(DebeziumOperandController.PLUGIN_NAME_PGOUTPUT);
                }

                if (MYSQL_CLASS.equals(connectorClass)) {
                    // Specifically test database history does not pass secrets directly
                    assertThat(kctr.getSpec().getConfig().get("database.history.consumer.sasl.jaas.config"))
                        .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + CLIENT_ID
                            + "\" password=\"${dir:/opt/kafka/external-configuration/"
                            + DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY
                            + ":"
                            + KAFKA_CLIENT_SECRET_KEY
                            + "}\";");
                    assertThat(kctr.getSpec().getConfig().get("database.history.producer.sasl.jaas.config"))
                        .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + CLIENT_ID
                            + "\" password=\"${dir:/opt/kafka/external-configuration/"
                            + DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY
                            + ":"
                            + KAFKA_CLIENT_SECRET_KEY
                            + "}\";");
                }
            });

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnect)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnect.class, kafkaConnectChecks);
    }

    @Test
    void testReifyWithSchemalessJson() {
        this.reify(PG_CLASS, addSchemalessJsonToConnectorConfig(getSpec()),
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

        this.reify(MYSQL_CLASS, addSchemalessJsonToConnectorConfig(getSpec()),
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
                CONFIGURATION.apicurioAuthServiceUrl());
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.service.url",
                CONFIGURATION.apicurioAuthServiceUrl());
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.realm", CONFIGURATION.apicurioAuthRealm());
            assertThat(kafkaConnect.getSpec().getConfig()).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.realm", CONFIGURATION.apicurioAuthRealm());
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
        this.reify(PG_CLASS, addAvroToConnectorConfig(getSpec()), getApicurioChecks(ApicurioAvroConverter.CONVERTER_CLASS));
        this.reify(MYSQL_CLASS, addAvroToConnectorConfig(getSpec()), getApicurioChecks(ApicurioAvroConverter.CONVERTER_CLASS));
    }

    @Test
    void testReifyWithJsonWithSchema() {
        this.reify(PG_CLASS, addJsonWithSchemaToConnectorConfig(getSpec()),
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

        this.reify(MYSQL_CLASS, addJsonWithSchemaToConnectorConfig(getSpec()),
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

    private static List<Condition> createConditions(
        String readyConditionStatus, String readyConditionReason, Condition otherCondition) {
        return List.of(createCondition("Ready", readyConditionStatus, readyConditionReason), otherCondition);
    }

    public static Stream<Arguments> computeStatus() {
        return Stream.of(
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("True", null),
                createConditions("True", null),
                ManagedConnector.STATE_READY,
                null, false),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("True", null),
                createConditions("False", "reason", "TimeoutException"),
                ManagedConnector.STATE_FAILED,
                "KafkaClusterUnreachable", false),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("True", null),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                "DebeziumException", true),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("False", "reason", "reason"),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                "reason", false),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                createConditions("False", "reason", "ConnectRestException"),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                "ConnectRestException", false),
            arguments(
                KafkaConnectorStatus.STATE_FAILED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_FAILED,
                "Bar", false),
            arguments(
                KafkaConnectorStatus.STATE_PAUSED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_STOPPED,
                "Paused", false),
            arguments(
                KafkaConnectorStatus.STATE_UNASSIGNED,
                List.of(createCondition("Foo", "True", "Bar")),
                createConditions("True", null),
                ManagedConnector.STATE_PROVISIONING,
                "Unassigned", false));
    }

    @ParameterizedTest
    @MethodSource
    void computeStatus(
        String connectorState,
        List<Condition> connectorConditions,
        List<Condition> connectConditions,
        String expectedManagedConnectorState,
        String expectedReason,
        boolean withDebeziumException) {

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
                    .addToConnectorStatus("tasks",
                        withDebeziumException ? List.of(Map.of("id", "0", "state", KafkaConnectorStatus.STATE_FAILED, "trace",
                            "io.debezium.DebeziumException: The connector is trying to read binlog starting at SourceInfo [currentGtid=null, currentBinlogFilename=mysql-bin-changelog.009801, currentBinlogPosition=157, currentRowNumber=0, serverId=0, sourceTime=null, threadId=-1, currentQuery=null, tableIds=[], databaseName=null], but this is no longer available on the server. Reconfigure the connector to use a snapshot when needed."))
                            : null)
                    .build())
                .build());

        assertThat(status.getPhase()).isEqualTo(expectedManagedConnectorState);

        assertThat(status.getConditions()).anySatisfy(condition -> assertThat(condition)
            .hasFieldOrPropertyWithValue("type", "Ready")
            .hasFieldOrPropertyWithValue("status", null == expectedReason ? "True" : "False")
            .hasFieldOrPropertyWithValue("reason", expectedReason));
    }
}
