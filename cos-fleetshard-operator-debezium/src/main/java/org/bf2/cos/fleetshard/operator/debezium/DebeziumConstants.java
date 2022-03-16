package org.bf2.cos.fleetshard.operator.debezium;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.support.CollectionUtils;

import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;

public final class DebeziumConstants {
    public static final String OPERATOR_TYPE = "debezium-connector-operator";
    public static final String OPERATOR_RUNTIME = "kafka-connect";

    public static final String EXTERNAL_CONFIG_DIRECTORY = "connector-configuration";
    public static final String EXTERNAL_CONFIG_FILE = "debezium-connector.properties";
    public static final String KAFKA_CLIENT_SECRET_KEY = "_kafka.client.secret";

    public static final String STRIMZI_DOMAIN = "strimzi.io/";
    public static final String STRIMZI_IO_USE_CONNECTOR_RESOURCES = STRIMZI_DOMAIN + "use-connector-resources";

    public static final String DEFAULT_IMAGE_PULL_SECRET_NAME = "addon-pullsecret";

    public static final Map<String, Object> DEFAULT_CONFIG_OPTIONS = CollectionUtils.mapOf(
        "request.timeout.ms", 20_000,
        "retry.backoff.ms", 500,
        "consumer.request.timeout.ms", 20_000,
        "consumer.retry.backoff.ms", 500,
        "producer.request.timeout.ms", 20_000,
        "producer.retry.backoff.ms", 500,
        "producer.compression.type", "lz4",
        "config.storage.replication.factor", -1,
        "offset.storage.replication.factor", -1,
        "status.storage.replication.factor", -1,
        "key.converter.schemas.enable", true,
        "value.converter.schemas.enable", true,
        "config.providers", "file,dir",
        "config.providers.file.class", "org.apache.kafka.common.config.provider.FileConfigProvider",
        "config.providers.dir.class", "org.apache.kafka.common.config.provider.DirectoryConfigProvider");

    public static final List<ResourceDefinitionContext> RESOURCE_TYPES = List.of(
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.RESOURCE_GROUP_NAME)
            .withVersion(KafkaConnect.CONSUMED_VERSION)
            .withKind(KafkaConnect.RESOURCE_KIND)
            .withPlural(KafkaConnect.RESOURCE_PLURAL)
            .build(),
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.RESOURCE_GROUP_NAME)
            .withVersion(KafkaConnector.CONSUMED_VERSION)
            .withKind(KafkaConnector.RESOURCE_KIND)
            .withPlural(KafkaConnector.RESOURCE_PLURAL)
            .build());

    private DebeziumConstants() {
    }

}
