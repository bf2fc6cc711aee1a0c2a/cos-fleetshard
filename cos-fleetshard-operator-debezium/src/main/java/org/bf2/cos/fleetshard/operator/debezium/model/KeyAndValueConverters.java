package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
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

    public static Map<String, Object> getConfig(DebeziumDataShape dataShape, ManagedConnector config,
        ServiceAccountSpec serviceAccountSpec) {
        if (null == dataShape) {
            LOGGER.error("Missing `data_shape` config in Debezium ManagedConnector \"" + config.getMetadata().getName()
                + "\"! Falling back to default.");
            dataShape = new DebeziumDataShape()
                .setKeyConverter(DebeziumDataShape.DEFAULT_CONVERTER_TYPE_KEY)
                .setValueConverter(DebeziumDataShape.DEFAULT_CONVERTER_TYPE_VALUE);
        }
        var converterConfig = new HashMap<>(Map.of(
            PROPERTY_KEY_CONVERTER, dataShape.getKeyConverter().getConverterClass(),
            PROPERTY_VALUE_CONVERTER, dataShape.getValueConverter().getConverterClass()));

        converterConfig.putAll(prefixMapKeys(PROPERTY_KEY_CONVERTER + ".",
            dataShape.getKeyConverter().getAdditionalConfig(config, serviceAccountSpec)));
        converterConfig.putAll(prefixMapKeys(PROPERTY_VALUE_CONVERTER + ".",
            dataShape.getValueConverter().getAdditionalConfig(config, serviceAccountSpec)));

        return new HashMap<>(converterConfig);
    }
}
