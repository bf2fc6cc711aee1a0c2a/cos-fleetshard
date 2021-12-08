package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.debezium")
public interface DebeziumOperandConfiguration {

    @WithDefault("org.apache.kafka.connect.json.JsonConverter")
    String keyConverter();

    @WithDefault("org.apache.kafka.connect.json.JsonConverter")
    String valueConverter();

    ContainerImage containerImage();

    KafkaConnect kafkaConnect();

    KafkaConnector kafkaConnector();

    interface ContainerImage {
        @WithDefault("image-registry.openshift-image-registry.svc:5000")
        String registry();

        @WithDefault("cos")
        String group();
    }

    interface KafkaConnect {
        Map<String, String> config();
    }

    interface KafkaConnector {
        Map<String, String> config();
    }
}
