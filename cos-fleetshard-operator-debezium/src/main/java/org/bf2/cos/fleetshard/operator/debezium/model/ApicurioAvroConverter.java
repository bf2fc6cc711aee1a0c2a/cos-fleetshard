package org.bf2.cos.fleetshard.operator.debezium.model;

public class ApicurioAvroConverter extends AbstractApicurioConverter implements KafkaConnectConverter {

    public static final String CONVERTER_CLASS = "io.apicurio.registry.utils.converter.AvroConverter";

    @Override
    public String getConverterClass() {
        return CONVERTER_CLASS;
    }
}
