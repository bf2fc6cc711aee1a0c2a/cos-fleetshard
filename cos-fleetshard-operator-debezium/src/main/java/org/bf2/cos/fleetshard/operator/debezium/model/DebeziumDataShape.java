package org.bf2.cos.fleetshard.operator.debezium.model;

import org.bf2.cos.fleetshard.operator.debezium.converter.KafkaConnectConverter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumDataShape {
    private String keyConverterString;
    private String valueConverterString;
    private KafkaConnectConverter keyConverter;
    private KafkaConnectConverter valueConverter;

    @JsonProperty("key")
    public String getKeyConverterAsString() {
        return keyConverterString;
    }

    @JsonProperty("key")
    public DebeziumDataShape setKeyConverter(String keyConverterString) {
        this.keyConverterString = keyConverterString;
        this.keyConverter = KafkaConnectConverter.createConverter(keyConverterString);
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
        this.valueConverter = KafkaConnectConverter.createConverter(valueConverterString);
        return this;
    }

    public KafkaConnectConverter getValueConverter() {
        return valueConverter;
    }
}
