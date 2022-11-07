package org.bf2.cos.fleetshard.support.resources;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import io.fabric8.kubernetes.api.model.ConfigMap;

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
}
