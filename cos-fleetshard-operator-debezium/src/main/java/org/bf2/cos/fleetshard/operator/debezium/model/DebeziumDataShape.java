package org.bf2.cos.fleetshard.operator.debezium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumDataShape {

    private String keyConverterString;
    private String valueConverterString;
    private KafkaConnectConverter keyConverter;
    private KafkaConnectConverter valueConverter;

    private static KafkaConnectConverter createConverter(String converterType) {
        switch (converterType) {
            case "JSON":
                return new ApicurioJsonConverter();
            case "AVRO":
                return new ApicurioAvroConverter();
            default:
            case "JSON_WITHOUT_SCHEMA":
            case "JSON without schema":
                return new KafkaConnectJsonConverter();
        }
    }

    @JsonProperty("key")
    public String getKeyConverterAsString() {
        return keyConverterString;
    }

    @JsonProperty("key")
    public void setKeyConverter(String keyConverterString) {
        this.keyConverterString = keyConverterString;
        this.keyConverter = createConverter(keyConverterString);
    }

    public KafkaConnectConverter getKeyConverter() {
        return keyConverter;
    }

    @JsonProperty("value")
    public String getValueConverterAsString() {
        return valueConverterString;
    }

    @JsonProperty("value")
    public void setValueConverter(String valueConverterString) {
        this.valueConverterString = valueConverterString;
        this.valueConverter = createConverter(valueConverterString);
    }

    public KafkaConnectConverter getValueConverter() {
        return valueConverter;
    }
}
