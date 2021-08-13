package org.bf2.cos.fleetshard.operator.debezium;

import java.util.List;

import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;

public final class DebeziumConstants {
    public static final String OPERATOR_TYPE = "debezium-connector-operator";

    public static final String EXTERNAL_CONFIG_DIRECTORY = "connector-configuration";
    public static final String EXTERNAL_CONFIG_FILE = "debezium-connector.properties";
    public static final String KAFKA_PASSWORD_SECRET_KEY = "_kafka.client.secret";

    public static final String STRIMZI_DOMAIN = "strimzi.io/";
    public static final String STRIMZI_IO_USE_CONNECTOR_RESOURCES = STRIMZI_DOMAIN + "use-connector-resources";

    public static final List<ResourceDefinitionContext> RESOURCE_TYPES = List.of(
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.STRIMZI_GROUP)
            .withVersion(KafkaConnect.CONSUMED_VERSION)
            .withKind(KafkaConnect.RESOURCE_KIND)
            .withPlural(KafkaConnect.RESOURCE_PLURAL)
            .build(),
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.STRIMZI_GROUP)
            .withVersion(KafkaConnector.CONSUMED_VERSION)
            .withKind(KafkaConnector.RESOURCE_KIND)
            .withPlural(KafkaConnector.RESOURCE_PLURAL)
            .build());

    private DebeziumConstants() {
    }

}
