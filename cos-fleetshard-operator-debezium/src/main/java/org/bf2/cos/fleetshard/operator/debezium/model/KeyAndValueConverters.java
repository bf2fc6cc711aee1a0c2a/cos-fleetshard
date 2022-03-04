package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;

public class KeyAndValueConverters {

    public static final String PROPERTY_KEY_CONVERTER = "key.converter";
    public static final String PROPERTY_VALUE_CONVERTER = "value.converter";

    private static Map<String, String> prefixMapKeys(String prefix, Map<String, String> map) {
        return map.entrySet().stream().map(property -> Map.entry(prefix + property.getKey(), property.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, Object> getConfig(DebeziumDataShape dataShape, ManagedConnector config,
        ServiceAccountSpec serviceAccountSpec) {
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
