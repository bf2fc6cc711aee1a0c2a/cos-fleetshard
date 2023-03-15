package org.bf2.cos.fleetshard.support.resources;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.utils.Serialization;

public final class ConfigMaps {
    public static String generateConnectorConfigMapId(String id) {
        String answer = id;

        if (!answer.startsWith(Resources.CONNECTOR_PREFIX)) {
            answer = Resources.CONNECTOR_PREFIX + answer;
        }
        if (!answer.endsWith(Resources.CONNECTOR_CONFIGMAP_SUFFIX)) {
            answer += Resources.CONNECTOR_CONFIGMAP_SUFFIX;
        }

        return answer;
    }

    public static String computeChecksum(ConfigMap configmap) {
        Checksum crc32 = new CRC32();

        configmap.getData().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                final byte[] k = entry.getKey().getBytes(StandardCharsets.UTF_8);
                final byte[] v = entry.getValue().getBytes(StandardCharsets.UTF_8);
                crc32.update(k, 0, k.length);
                crc32.update(v, 0, v.length);
            });

        return Long.toHexString(crc32.getValue());
    }

    public static ConfigMap set(ConfigMap target, String key, Properties props) {
        return set(target, key, props, Collections.emptyMap());
    }

    public static ConfigMap set(ConfigMap target, String key, Properties props, Map<String, String> overrides) {
        if (target == null || key == null || props == null) {
            return target;
        }

        if (target.getData() == null) {
            target.setData(new TreeMap<>());
        }

        final Properties all;

        if (overrides.isEmpty()) {
            all = props;
        } else {
            all = new Properties();
            all.putAll(props);
            all.putAll(overrides);
        }

        String val = all.keySet().stream()
            .sorted()
            .map(k -> k + "=" + all.getProperty((String) k))
            .collect(Collectors.joining("\n"));

        target.getData().put(
            key,
            val);

        return target;
    }

    public static ConfigMap set(ConfigMap target, String key, String val) {
        if (target == null || key == null || val == null) {
            return target;
        }

        if (target.getData() == null) {
            target.setData(new TreeMap<>());
        }

        target.getData().put(
            key,
            val);

        return target;
    }

    @SuppressWarnings("unchecked")
    public static <T> T extract(ConfigMap source, String key, Class<T> type) {
        if (source == null) {
            return null;
        }
        if (source.getData() == null) {
            return null;
        }

        String val = source.getData().get(key);
        if (val == null) {
            return null;
        }

        if (type.isAssignableFrom(String.class)) {
            return (T) val;
        }

        if (type.isAssignableFrom(Properties.class)) {
            Properties result = new Properties();
            try {
                result.load(new StringReader(val));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return (T) result;
        }

        return Serialization.unmarshal(val, type);
    }

}
