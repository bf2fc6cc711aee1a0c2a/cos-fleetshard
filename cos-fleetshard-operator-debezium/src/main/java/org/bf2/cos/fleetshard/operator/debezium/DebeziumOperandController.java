package org.bf2.cos.fleetshard.operator.debezium;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.KeyAndValueConverters;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.strimzi.api.kafka.model.ClientTlsBuilder;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.ExternalConfigurationReferenceBuilder;
import io.strimzi.api.kafka.model.InlineLoggingBuilder;
import io.strimzi.api.kafka.model.JmxPrometheusExporterMetricsBuilder;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.KafkaConnectSpecBuilder;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.KafkaConnectorSpecBuilder;
import io.strimzi.api.kafka.model.PasswordSecretSourceBuilder;
import io.strimzi.api.kafka.model.authentication.KafkaClientAuthenticationPlainBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationVolumeSourceBuilder;
import io.strimzi.api.kafka.model.template.KafkaConnectTemplateBuilder;
import io.strimzi.api.kafka.model.template.PodTemplateBuilder;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_CLIENT_SECRET_KEY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RESOURCE_TYPES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_DOMAIN;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_IO_USE_CONNECTOR_RESOURCES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.computeStatus;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createConfig;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createSecretsData;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.lookupConnector;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.lookupKafkaConnect;
import static org.bf2.cos.fleetshard.support.CollectionUtils.asBytesBase64;

@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class DebeziumOperandController extends AbstractOperandController<DebeziumShardMetadata, ObjectNode, DebeziumDataShape> {

    public static final String METRICS_CONFIG_FILENAME = "kafka_connect_metrics.yml";
    public static final String METRICS_CONFIG; // this could eventually go to the bundle

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumOperandController.class);
    public static final String KAFKA_CONNECT_METRICS_CONFIGMAP_NAME_SUFFIX = "-metrics";
    public static final String CLASS_NAME_POSTGRES_CONNECTOR = "io.debezium.connector.postgresql.PostgresConnector";
    public static final String CLASS_NAME_MONGODB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
    public static final String CLASS_NAME_SQLSERVER_CONNECTOR = "io.debezium.connector.sqlserver.SqlServerConnector";
    public static final String CONFIG_OPTION_POSTGRES_PLUGIN_NAME = "plugin.name";
    public static final String PLUGIN_NAME_PGOUTPUT = "pgoutput";

    static {
        try {
            METRICS_CONFIG = new String(Objects
                .requireNonNull(DebeziumOperandController.class.getClassLoader().getResourceAsStream(METRICS_CONFIG_FILENAME))
                .readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final DebeziumOperandConfiguration configuration;

    public DebeziumOperandController(
        FleetShardOperatorConfig config,
        KubernetesClient kubernetesClient,
        DebeziumOperandConfiguration configuration) {

        super(config, kubernetesClient, DebeziumShardMetadata.class, ObjectNode.class, DebeziumDataShape.class);

        this.configuration = configuration;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return RESOURCE_TYPES;
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnector connector,
        DebeziumShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, DebeziumDataShape> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        final Map<String, String> secretsData = createSecretsData(connectorConfiguration.getConnectorSpec());

        final Secret secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX)
                .build())
            .addToData(EXTERNAL_CONFIG_FILE, asBytesBase64(secretsData))
            .addToData(KAFKA_CLIENT_SECRET_KEY, serviceAccountSpec.getClientSecret())
            .build();

        ConfigMap kafkaConnectMetricsConfigMap = new ConfigMapBuilder()
            .withNewMetadata().withName(connector.getMetadata().getName() + KAFKA_CONNECT_METRICS_CONFIGMAP_NAME_SUFFIX)
            .endMetadata()
            .addToData(METRICS_CONFIG_FILENAME, METRICS_CONFIG)
            .build();

        final Map<String, String> loggingMap = new HashMap<>();
        connectorConfiguration.extractOverrideProperties()
            .forEach((k, v) -> loggingMap.put(k.toString(), v.toString()));

        final KafkaConnectSpecBuilder kcsb = new KafkaConnectSpecBuilder()
            .withReplicas(1)
            .withBootstrapServers(connector.getSpec().getDeployment().getKafka().getUrl())
            .withKafkaClientAuthenticationPlain(new KafkaClientAuthenticationPlainBuilder()
                .withUsername(serviceAccountSpec.getClientId())
                .withPasswordSecret(new PasswordSecretSourceBuilder()
                    .withSecretName(secret.getMetadata().getName())
                    .withPassword(KAFKA_CLIENT_SECRET_KEY)
                    .build())
                .build())
            .addToConfig(DebeziumConstants.DEFAULT_CONFIG_OPTIONS)
            // add external configuration
            .addToConfig(new TreeMap<>(configuration.kafkaConnect().config()))
            .addToConfig("group.id", connector.getMetadata().getName())
            // converters
            .addToConfig(
                KeyAndValueConverters.getConfig(connectorConfiguration.getDataShapeSpec(), connector, serviceAccountSpec,
                    configuration))
            // topics
            .addToConfig("offset.storage.topic", connector.getMetadata().getName() + "-offset")
            .addToConfig("config.storage.topic", connector.getMetadata().getName() + "-config")
            .addToConfig("status.storage.topic", connector.getMetadata().getName() + "-status")
            .addToConfig("topic.creation.enable", "true")
            .addToConfig("producer.compression.type", "lz4")
            // added to trigger a re-deployment if the secret changes
            .addToConfig("connector.secret.name", secret.getMetadata().getName())
            .addToConfig("connector.secret.checksum", Secrets.computeChecksum(secret))
            .withTls(new ClientTlsBuilder()
                .withTrustedCertificates(Collections.emptyList())
                .build())
            .withTemplate(new KafkaConnectTemplateBuilder()
                .withPod(new PodTemplateBuilder()
                    .withImagePullSecrets(configuration.imagePullSecretsName())
                    .build())
                .build())
            .withJmxPrometheusExporterMetricsConfig(
                new JmxPrometheusExporterMetricsBuilder()
                    .withValueFrom(
                        new ExternalConfigurationReferenceBuilder()
                            .withNewConfigMapKeyRef(
                                METRICS_CONFIG_FILENAME,
                                kafkaConnectMetricsConfigMap.getMetadata().getName(),
                                false)
                            .build())
                    .build())
            .withExternalConfiguration(new ExternalConfigurationBuilder()
                .addToVolumes(new ExternalConfigurationVolumeSourceBuilder()
                    .withName(EXTERNAL_CONFIG_DIRECTORY)
                    .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(secret.getMetadata().getName())
                        .build())
                    .build())
                .build())
            .withResources(new ResourceRequirementsBuilder()
                .addToRequests("cpu", new Quantity("10m"))
                .addToRequests("memory", new Quantity("256Mi"))
                .addToLimits("cpu", new Quantity("500m"))
                .addToLimits("memory", new Quantity("1Gi"))
                .build())
            .withLogging(new InlineLoggingBuilder()
                .withLoggers(loggingMap)
                .build());

        kcsb.withImage(shardMetadata.getContainerImage());

        final KafkaConnect kc = new KafkaConnectBuilder()
            .withApiVersion(Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnect.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .addToAnnotations(STRIMZI_IO_USE_CONNECTOR_RESOURCES, "true")
                .build())
            .withSpec(kcsb.build())
            .build();

        Map<String, Object> connectorConfig = createConfig(configuration, connectorConfiguration.getConnectorSpec());

        // handle connector config defaults
        switch (shardMetadata.getConnectorClass()) {
            case CLASS_NAME_POSTGRES_CONNECTOR:
                if (!connectorConfig.containsKey(CONFIG_OPTION_POSTGRES_PLUGIN_NAME)) {
                    connectorConfig.put(CONFIG_OPTION_POSTGRES_PLUGIN_NAME, PLUGIN_NAME_PGOUTPUT);
                }
                break;
            default:
                break;
        }

        if (isDatabaseStorageSupported(shardMetadata)) {
            final Map<String, Object> databaseStorageConfigs = new LinkedHashMap<>();

            databaseStorageConfigs.put("schema.history.internal.kafka.bootstrap.servers",
                connector.getSpec().getDeployment().getKafka().getUrl());
            databaseStorageConfigs.put("schema.history.internal.kafka.topic",
                connector.getMetadata().getName() + "-schema-history");
            databaseStorageConfigs.put("schema.history.internal.producer.security.protocol", "SASL_SSL");
            databaseStorageConfigs.put("schema.history.internal.consumer.security.protocol", "SASL_SSL");
            databaseStorageConfigs.put("schema.history.internal.producer.sasl.mechanism", "PLAIN");
            databaseStorageConfigs.put("schema.history.internal.consumer.sasl.mechanism", "PLAIN");
            databaseStorageConfigs.put("schema.history.internal.producer.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                    + serviceAccountSpec.getClientId()
                    + "\" password=\""
                    + "${dir:/opt/kafka/external-configuration/"
                    + EXTERNAL_CONFIG_DIRECTORY
                    + ":"
                    + KAFKA_CLIENT_SECRET_KEY
                    + "}\";");
            databaseStorageConfigs.put("schema.history.internal.consumer.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                    + serviceAccountSpec.getClientId()
                    + "\" password=\""
                    + "${dir:/opt/kafka/external-configuration/"
                    + EXTERNAL_CONFIG_DIRECTORY
                    + ":"
                    + KAFKA_CLIENT_SECRET_KEY
                    + "}\";");

            connectorConfig.putAll(databaseStorageConfigs);
        }

        if (isSQLServerConnector(shardMetadata)) {
            connectorConfig.put("database.encrypt", false);
        }

        final KafkaConnector kctr = new KafkaConnectorBuilder()
            .withApiVersion(Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnector.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .addToLabels(STRIMZI_DOMAIN + "cluster", connector.getMetadata().getName())
                .build())
            .withSpec(new KafkaConnectorSpecBuilder()
                .withClassName(shardMetadata.getConnectorClass())
                .withTasksMax(1)
                .withPause(false)
                .withConfig(connectorConfig)
                .addToConfig("topic.creation.default.replication.factor", -1)
                .addToConfig("topic.creation.default.partitions", -1)
                .addToConfig("topic.creation.default.cleanup.policy", "compact")
                .addToConfig("topic.creation.default.delete.retention.ms", 2_678_400_000L)
                .build())
            .build();

        getFleetShardOperatorConfig().metrics().recorder().tags().labels().stream().flatMap(List::stream).forEach(k -> {
            String v = Resources.getLabel(connector, k);
            if (v != null) {
                Resources.setLabel(kc, k, v);
                Resources.setLabel(kctr, k, v);
            }
        });
        getFleetShardOperatorConfig().metrics().recorder().tags().annotations().stream().flatMap(List::stream).forEach(k -> {
            String v = Resources.getAnnotation(connector, k);
            if (v != null) {
                Resources.setAnnotation(kc, k, v);
                Resources.setAnnotation(kctr, k, v);
            }
        });

        return List.of(secret, kafkaConnectMetricsConfigMap, kc, kctr);
    }

    @Override
    public void status(ManagedConnector connector) {
        KafkaConnector kctr = lookupConnector(getKubernetesClient(), connector)
            .filter(_kctr -> _kctr.getStatus() != null).orElse(null);
        KafkaConnect kc = lookupKafkaConnect(getKubernetesClient(), connector)
            .filter(_kc -> _kc.getStatus() != null).orElse(null);
        computeStatus(connector.getStatus().getConnectorStatus(), kc, kctr);
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return delete(connector);
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        Boolean kctr = Resources.delete(
            getKubernetesClient(),
            KafkaConnector.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());
        Boolean kc = Resources.delete(
            getKubernetesClient(),
            KafkaConnect.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());
        Boolean secret = Resources.delete(
            getKubernetesClient(),
            Secret.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX);

        LOGGER.debug("deleting connector {}/{} (KafkaConnector: {}, KafkaConnect: {}, Secret: {})",
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            kctr,
            kc,
            secret);

        return kctr && kc && secret;
    }

    private boolean isDatabaseStorageSupported(DebeziumShardMetadata shardMetadata) {
        switch (shardMetadata.getConnectorClass()) {
            case CLASS_NAME_MONGODB_CONNECTOR:
            case CLASS_NAME_POSTGRES_CONNECTOR:
                return false;
            default:
                return true;
        }
    }

    private boolean isSQLServerConnector(DebeziumShardMetadata shardMetadata) {
        return shardMetadata.getConnectorClass().equals(CLASS_NAME_SQLSERVER_CONNECTOR);
    }
}
