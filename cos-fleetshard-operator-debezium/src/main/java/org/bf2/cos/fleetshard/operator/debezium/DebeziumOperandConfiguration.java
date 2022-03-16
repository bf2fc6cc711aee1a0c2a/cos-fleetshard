package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "cos.operator.debezium")
public interface DebeziumOperandConfiguration {

    @ConfigProperty(name = "image_pull_secrets_name", defaultValue = DebeziumConstants.DEFAULT_IMAGE_PULL_SECRET_NAME)
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
