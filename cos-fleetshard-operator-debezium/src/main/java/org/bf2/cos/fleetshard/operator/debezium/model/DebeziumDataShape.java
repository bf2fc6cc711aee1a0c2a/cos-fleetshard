package org.bf2.cos.fleetshard.operator.debezium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumDataShape {

    public static final String CONVERTER_TYPE_JSON = "JSON";
    public static final String CONVERTER_TYPE_AVRO = "AVRO";
    public static final String CONVERTER_TYPE_JSON_WITHOUT_SCHEMA = "JSON without schema";
    public static final String DEFAULT_CONVERTER_TYPE_KEY = CONVERTER_TYPE_JSON_WITHOUT_SCHEMA;
    public static final String DEFAULT_CONVERTER_TYPE_VALUE = CONVERTER_TYPE_JSON_WITHOUT_SCHEMA;

    private String keyConverterString;
    private String valueConverterString;
    private KafkaConnectConverter keyConverter;
    private KafkaConnectConverter valueConverter;

    private static KafkaConnectConverter createConverter(String converterType) {
        switch (converterType) {
            case CONVERTER_TYPE_JSON:
                return new ApicurioJsonConverter();
            case CONVERTER_TYPE_AVRO:
                return new ApicurioAvroConverter();
            default:
            case "JSON_WITHOUT_SCHEMA":
            case CONVERTER_TYPE_JSON_WITHOUT_SCHEMA:
                return new KafkaConnectJsonConverter();
        }
    }

    @JsonProperty("key")
    public String getKeyConverterAsString() {
        return keyConverterString;
    }

    @JsonProperty("key")
    public DebeziumDataShape setKeyConverter(String keyConverterString) {
        this.keyConverterString = keyConverterString;
        this.keyConverter = createConverter(keyConverterString);
        return this;
    }

    public KafkaConnectConverter getKeyConverter() {
        return keyConverter;
    }

    @JsonProperty("value")
    public String getValueConverterAsString() {
        return valueConverterString;
    }

    @JsonProperty("value")
    public DebeziumDataShape setValueConverter(String valueConverterString) {
        this.valueConverterString = valueConverterString;
        this.valueConverter = createConverter(valueConverterString);
        return this;
    }

    public KafkaConnectConverter getValueConverter() {
        return valueConverter;
    }
}
