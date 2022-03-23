package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Map;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.debezium")
public interface DebeziumOperandConfiguration {

    @WithDefault(DebeziumConstants.DEFAULT_IMAGE_PULL_SECRET_NAME)
    LocalObjectReference imagePullSecretsName();

    KafkaConnect kafkaConnect();

    KafkaConnector kafkaConnector();

    interface KafkaConnect {
        Map<String, String> config();
    }

    interface KafkaConnector {
        Map<String, String> config();
    }
}
