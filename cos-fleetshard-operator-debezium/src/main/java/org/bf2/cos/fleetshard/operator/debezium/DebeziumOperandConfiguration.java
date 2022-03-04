package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "cos.operator.debezium")
public interface DebeziumOperandConfiguration {

    KafkaConnect kafkaConnect();

    KafkaConnector kafkaConnector();

    interface KafkaConnect {
        Map<String, String> config();
    }

    interface KafkaConnector {
        Map<String, String> config();
    }
}
