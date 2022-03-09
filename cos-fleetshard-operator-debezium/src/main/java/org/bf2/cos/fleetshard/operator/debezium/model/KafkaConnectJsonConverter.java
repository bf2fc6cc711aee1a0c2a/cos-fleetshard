package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;

public class KafkaConnectJsonConverter implements KafkaConnectConverter {

    public static final String CONVERTER_CLASS = "org.apache.kafka.connect.json.JsonConverter";

    @Override
    public String getConverterClass() {
        return CONVERTER_CLASS;
    }

    @Override
    public Map<String, String> getAdditionalConfig(ManagedConnector config, ServiceAccountSpec serviceAccountSpec) {
        return Map.of("schemas.enable", "false");
    }
}
