package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ServiceAccountSpecBuilder;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorClient;
import org.bf2.cos.fleetshard.operator.debezium.converter.ApicurioAvroConverter;
import org.bf2.cos.fleetshard.operator.debezium.converter.KafkaConnectJsonConverter;
import org.bf2.cos.fleetshard.operator.debezium.converter.KeyAndValueConverters;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumShardMetadataBuilder;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorderConfig;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECTOR_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECT_CONFIG_FILENAME;
import static org.mockito.Mockito.when;

public class DebeziumOperandControllerTest {
    private static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid";
    private static final Long DEFAULT_CONNECTOR_REVISION = 1L;
    private static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid";
    private static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/pg:1";
    private static final String DEFAULT_DEPLOYMENT_ID = "1";
    private static final String DEFAULT_CLUSTER_ID = "cid";
    private static final Long DEFAULT_DEPLOYMENT_REVISION = 1L;
    private static final String CLIENT_ID = "kcid";
    private static final String CLIENT_SECRET_RAW = "kcs";
    private static final String CLIENT_SECRET = Base64.getEncoder()
        .encodeToString(CLIENT_SECRET_RAW.getBytes(StandardCharsets.UTF_8));
    private static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181";
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
            return new KafkaConnect() {
                @Override
                public Map<String, String> config() {
                    return Collections.emptyMap();
                }

                @Override
                public Offset offset() {
                    return new Offset() {
                        @Override
                        public Quantity storage() {
                            return new Quantity("1Mi");
                        }
                    };
                }
            };
        }

        @Override
        public KafkaConnector kafkaConnector() {
            return Map::of;
        }
    };

    private static FleetShardOperatorConfig fleetShardOperatorConfig() {
        var config = Mockito.mock(FleetShardOperatorConfig.class);
        var metrics = Mockito.mock(FleetShardOperatorConfig.Metrics.class);
        var recorder = Mockito.mock(MetricsRecorderConfig.class);
        var tags = Mockito.mock(MetricsRecorderConfig.Tags.class);

        when(tags.common())
            .thenAnswer(invocation -> Map.of());
        when(tags.annotations())
            .thenAnswer(invocation -> Optional.of(List.of("my.cos.bf2.org/connector-group")));
        when(tags.labels())
            .thenAnswer(invocation -> Optional.of(List.of("cos.bf2.org/organization-id", "cos.bf2.org/pricing-tier")));

        when(recorder.tags())
            .thenAnswer(invocation -> tags);
        when(metrics.recorder())
            .thenAnswer(invocation -> recorder);
        when(config.metrics())
            .thenAnswer(invocation -> metrics);

        return config;

    }

    @Test
    void declaresExpectedResourceTypes() {
        FleetShardOperatorConfig config = fleetShardOperatorConfig();
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(config, kubernetesClient, CONFIGURATION, null);

        assertThat(controller.getResourceTypes())
            .hasSize(1)
            .first()
            .matches(r -> "apps".equals(r.getGroup()) && "v1".equals(r.getVersion()) && "Deployment".equals(r.getKind()));
    }

    @Test
    void testReifyWithSchemalessJson() {
        this.reify(DebeziumConstants.CLASS_NAME_POSTGRES_CONNECTOR, addSchemalessJsonToConnectorConfig(getSpec()), result -> {
            Properties connectProperties = Secrets.extract(result.secret, CONNECT_CONFIG_FILENAME, Properties.class);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable",
                "false");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable",
                "false");
        });

        this.reify(DebeziumConstants.CLASS_NAME_MYSQL_CONNECTOR, addSchemalessJsonToConnectorConfig(getSpec()), result -> {
            Properties connectProperties = Secrets.extract(result.secret, CONNECT_CONFIG_FILENAME, Properties.class);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable",
                "false");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable",
                "false");
        });
    }

    @Test
    void testReifyWithAvro() {
        this.reify(
            DebeziumConstants.CLASS_NAME_POSTGRES_CONNECTOR,
            addAvroToConnectorConfig(getSpec()),
            getApicurioChecks(ApicurioAvroConverter.CONVERTER_CLASS));

        this.reify(
            DebeziumConstants.CLASS_NAME_MYSQL_CONNECTOR,
            addAvroToConnectorConfig(getSpec()),
            getApicurioChecks(ApicurioAvroConverter.CONVERTER_CLASS));
    }

    private Consumer<ReifyResult> getApicurioChecks(String converterClass) {
        return result -> {

            Properties connectProperties = Secrets.extract(result.secret, CONNECT_CONFIG_FILENAME, Properties.class);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                converterClass);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                converterClass);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.service.url",
                CONFIGURATION.apicurioAuthServiceUrl());

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.service.url",
                CONFIGURATION.apicurioAuthServiceUrl());

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.realm",
                CONFIGURATION.apicurioAuthRealm());
            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.realm",
                CONFIGURATION.apicurioAuthRealm());

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.url",
                SCHEMA_REGISTRY_URL);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.url",
                SCHEMA_REGISTRY_URL);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.client.id",
                CLIENT_ID);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.client.id",
                CLIENT_ID);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.auth.client.secret",
                CLIENT_SECRET_RAW);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.auth.client.secret",
                CLIENT_SECRET_RAW);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.auto-register", "true");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.auto-register", "true");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".apicurio.registry.find-latest", "true");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".apicurio.registry.find-latest", "true");
        };
    }

    @Test
    void testReifyWithJsonWithSchema() {
        this.reify(DebeziumConstants.CLASS_NAME_POSTGRES_CONNECTOR, addJsonWithSchemaToConnectorConfig(getSpec()), result -> {
            Properties connectProperties = Secrets.extract(result.secret, CONNECT_CONFIG_FILENAME, Properties.class);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable",
                "true");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable",
                "true");
        });

        this.reify(DebeziumConstants.CLASS_NAME_MYSQL_CONNECTOR, addJsonWithSchemaToConnectorConfig(getSpec()), result -> {
            Properties connectProperties = Secrets.extract(result.secret, CONNECT_CONFIG_FILENAME, Properties.class);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_KEY_CONVERTER + ".schemas.enable",
                "true");

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER,
                KafkaConnectJsonConverter.CONVERTER_CLASS);

            assertThat(connectProperties).containsEntry(
                KeyAndValueConverters.PROPERTY_VALUE_CONVERTER + ".schemas.enable",
                "true");
        });
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
        spec.withObject("/database.password").put("kind", "base64").put("value", pwdB64);

        return spec;
    }

    private ObjectNode addAvroToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.withObject("/data_shape").put("key", "AVRO").put("value", "AVRO");
        return baseConfig;
    }

    private ObjectNode addJsonWithSchemaToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.withObject("/data_shape").put("key", "JSON").put("value", "JSON");
        return baseConfig;
    }

    private ObjectNode addSchemalessJsonToConnectorConfig(ObjectNode baseConfig) {
        baseConfig.withObject("/data_shape").put("key", "JSON without schema").put("value", "JSON without schema");
        return baseConfig;
    }

    public static class ReifyResult {
        public final Deployment deployment;
        public final Secret secret;
        public final ConfigMap configMap;
        public final PersistentVolumeClaim persistentVolume;
        public final Service service;

        public ReifyResult(Deployment deployment, Secret secret, ConfigMap configMap, PersistentVolumeClaim persistentVolume,
            Service service) {
            this.deployment = deployment;
            this.secret = secret;
            this.configMap = configMap;
            this.persistentVolume = persistentVolume;
            this.service = service;
        }

        public static ReifyResult of(List<HasMetadata> resources) {
            return new ReifyResult(
                resources.stream()
                    .filter(Resources::isDeployment)
                    .findFirst()
                    .map(Deployment.class::cast)
                    .orElse(null),
                resources.stream()
                    .filter(Resources::isSecret)
                    .findFirst()
                    .map(Secret.class::cast)
                    .orElse(null),
                resources.stream()
                    .filter(Resources::isConfigMap)
                    .findFirst()
                    .map(ConfigMap.class::cast)
                    .orElse(null),
                resources.stream()
                    .filter(Resources::isPersistentVolumeClaim)
                    .findFirst()
                    .map(PersistentVolumeClaim.class::cast)
                    .orElse(null),
                resources.stream()
                    .filter(Resources::isService)
                    .findFirst()
                    .map(Service.class::cast)
                    .orElse(null));
        }
    }

    void reify(
        String connectorClass,
        ObjectNode connectorConfig,
        Consumer<ReifyResult> resultConsumer) {

        FleetShardOperatorConfig config = fleetShardOperatorConfig();
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        KafkaConnectorClient konnectorClient = Mockito.mock(KafkaConnectorClient.class);

        DebeziumOperandController controller = new DebeziumOperandController(
            config,
            kubernetesClient,
            CONFIGURATION,
            konnectorClient);

        var resources = controller.doReify(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withNamespace(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withUid(MANAGED_CONNECTOR_UID)
                    .addToAnnotations("my.cos.bf2.org/connector-group", "foo")
                    .addToLabels("cos.bf2.org/organization-id", "20000000")
                    .addToLabels("cos.bf2.org/pricing-tier", "essential")
                    .addToLabels(Resources.LABEL_CLUSTER_ID, DEFAULT_CLUSTER_ID)
                    .addToLabels(Resources.LABEL_CONNECTOR_ID, DEFAULT_MANAGED_CONNECTOR_ID)
                    .addToLabels(Resources.LABEL_DEPLOYMENT_ID, DEFAULT_DEPLOYMENT_ID)
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withClusterId(DEFAULT_CLUSTER_ID)
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
            new DebeziumShardMetadataBuilder()
                .withContainerImage(DEFAULT_CONNECTOR_IMAGE)
                .withConnectorClass(connectorClass)
                .build(),
            new ConnectorConfiguration<>(connectorConfig, ObjectNode.class,
                DebeziumDataShape.class, null),
            new ServiceAccountSpecBuilder()
                .withClientId(CLIENT_ID)
                .withClientSecret(CLIENT_SECRET)
                .build());

        assertThat(resources)
            .anyMatch(Resources::isDeployment)
            .anyMatch(Resources::isPersistentVolumeClaim)
            .anyMatch(Resources::isSecret)
            .anyMatch(Resources::isConfigMap)
            .anyMatch(Resources::isService);

        ReifyResult result = ReifyResult.of(resources);
        assertThat(result)
            .hasNoNullFieldsOrProperties();

        assertThat(result.configMap.getData()).containsEntry(
            DebeziumConstants.METRICS_CONFIG_FILENAME,
            DebeziumConstants.METRICS_CONFIG);

        assertThat(result.deployment).satisfies(d -> {
            assertThat(d.getSpec().getTemplate().getSpec().getContainers()).first().satisfies(c -> {
                assertThat(c.getImage()).isEqualTo(DEFAULT_CONNECTOR_IMAGE);
            });
            assertThat(d.getSpec().getTemplate().getSpec().getImagePullSecrets())
                .contains(CONFIGURATION.imagePullSecretsName());
        });

        assertThat(result.secret).satisfies(s -> {
            Properties connectorProperties = Secrets.extract(s, CONNECTOR_CONFIG_FILENAME, Properties.class);

            assertThat(connectorProperties).containsEntry(
                "database.password",
                "orderpw");

            if (DebeziumConstants.CLASS_NAME_POSTGRES_CONNECTOR.equals(connectorClass)) {
                // Specifically test the plugin name for PostgreSQL
                assertThat(connectorProperties).containsEntry(
                    DebeziumConstants.CONFIG_OPTION_POSTGRES_PLUGIN_NAME,
                    DebeziumConstants.PLUGIN_NAME_PGOUTPUT);
            }

            if (DebeziumConstants.CLASS_NAME_MYSQL_CONNECTOR.equals(connectorClass)) {
                // Specifically test database history does not pass secrets directly
                assertThat(connectorProperties).containsEntry(
                    "schema.history.internal.consumer.sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                        + CLIENT_ID
                        + "\" password=\"" + CLIENT_SECRET_RAW + "\";");
                assertThat(connectorProperties).containsEntry(
                    "schema.history.internal.producer.sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                        + CLIENT_ID
                        + "\" password=\"" + CLIENT_SECRET_RAW + "\";");
            }
        });

        assertThat(result)
            .satisfies(resultConsumer);
    }
}
