package org.bf2.cos.fleetshard.operator.debezium.converter;

import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandConfiguration;

public interface KafkaConnectConverter {

    String CONVERTER_TYPE_JSON = "JSON";
    String CONVERTER_TYPE_AVRO = "AVRO";
    String CONVERTER_TYPE_JSON_WITHOUT_SCHEMA = "JSON without schema";
    String DEFAULT_CONVERTER_TYPE_KEY = CONVERTER_TYPE_JSON_WITHOUT_SCHEMA;
    String DEFAULT_CONVERTER_TYPE_VALUE = CONVERTER_TYPE_JSON_WITHOUT_SCHEMA;

    static KafkaConnectConverter createConverter(String converterType) {
        switch (converterType) {
            case CONVERTER_TYPE_JSON:
                return new KafkaConnectJsonWithSchemaConverter();
            case CONVERTER_TYPE_AVRO:
                return new ApicurioAvroConverter();
            default:
            case CONVERTER_TYPE_JSON_WITHOUT_SCHEMA:
                return new KafkaConnectJsonConverter();
        }
    }

    String getConverterClass();

    Map<String, String> getAdditionalConfig(ManagedConnector config, ServiceAccountSpec serviceAccountSpec,
        DebeziumOperandConfiguration configuration);
}
