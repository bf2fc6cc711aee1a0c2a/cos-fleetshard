package org.bf2.cos.fleetshard.operator.debezium.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyAndValueConverters {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyAndValueConverters.class);
    public static final String PROPERTY_KEY_CONVERTER = "key.converter";
    public static final String PROPERTY_VALUE_CONVERTER = "value.converter";

    private static Map<String, String> prefixMapKeys(String prefix, Map<String, String> map) {
        return map.entrySet().stream().map(property -> Map.entry(prefix + property.getKey(), property.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, String> getConfig(DebeziumDataShape dataShape, ManagedConnector config,
        ServiceAccountSpec serviceAccountSpec, DebeziumOperandConfiguration configuration) {

        if (null == dataShape) {
            LOGGER.info("Missing `data_shape` config in Debezium ManagedConnector \"" + config.getMetadata().getName()
                + "\"! Falling back to default.");

            dataShape = new DebeziumDataShape()
                .setKeyConverter(KafkaConnectConverter.DEFAULT_CONVERTER_TYPE_KEY)
                .setValueConverter(KafkaConnectConverter.DEFAULT_CONVERTER_TYPE_VALUE);
        }

        Map<String, String> converterConfig = new HashMap<>();
        converterConfig.put(PROPERTY_KEY_CONVERTER, dataShape.getKeyConverter().getConverterClass());
        converterConfig.put(PROPERTY_VALUE_CONVERTER, dataShape.getValueConverter().getConverterClass());

        converterConfig.putAll(prefixMapKeys(PROPERTY_KEY_CONVERTER + ".",
            dataShape.getKeyConverter().getAdditionalConfig(config, serviceAccountSpec, configuration)));
        converterConfig.putAll(prefixMapKeys(PROPERTY_VALUE_CONVERTER + ".",
            dataShape.getValueConverter().getAdditionalConfig(config, serviceAccountSpec, configuration)));

        return new HashMap<>(converterConfig);
    }
}
