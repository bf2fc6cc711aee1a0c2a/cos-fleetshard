package org.bf2.cos.fleetshard.support.resources;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

public final class Secrets {
    public static final String SECRET_ENTRY_CONNECTOR = "connector";
    public static final String SECRET_ENTRY_PROCESSOR = "processor";
    public static final String SECRET_ENTRY_SERVICE_ACCOUNT = "serviceAccount";
    public static final String SECRET_ENTRY_META = "meta";

    private Secrets() {
    }

    public static String computeChecksum(Secret secret) {
        Checksum crc32 = new CRC32();

        secret.getData().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                final byte[] k = entry.getKey().getBytes(StandardCharsets.UTF_8);
                final byte[] v = entry.getValue().getBytes(StandardCharsets.UTF_8);
                crc32.update(k, 0, k.length);
                crc32.update(v, 0, v.length);
            });

        return Long.toHexString(crc32.getValue());
    }

    public static ObjectNode extract(Secret secret, String key) {
        if (secret == null) {
            return Serialization.jsonMapper().createObjectNode();
        }
        if (secret.getData() == null) {
            return Serialization.jsonMapper().createObjectNode();
        }
        String val = secret.getData().get(key);
        if (val == null) {
            return Serialization.jsonMapper().createObjectNode();
        }

        return extract(secret, key, ObjectNode.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T extract(Secret secret, String key, Class<T> type) {
        if (secret == null) {
            return null;
        }
        if (secret.getData() == null) {
            return null;
        }
        String val = secret.getData().get(key);
        if (val == null) {
            return null;
        }

        String decoded = new String(Base64.getDecoder().decode(val), StandardCharsets.UTF_8);

        if (type.isAssignableFrom(String.class)) {
            return (T) decoded;
        }

        if (type.isAssignableFrom(Properties.class)) {
            Properties result = new Properties();
            try {
                result.load(new StringReader(decoded));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return (T) result;
        }

        return Serialization.unmarshal(decoded, type);
    }

    public static Secret set(Secret secret, String key, String val) {
        if (secret == null || key == null || val == null) {
            return secret;
        }

        if (secret.getData() == null) {
            secret.setData(new HashMap<>());
        }

        secret.getData().put(
            key,
            Base64.getEncoder().encodeToString(val.getBytes(StandardCharsets.UTF_8)));

        return secret;
    }

    public static Secret set(Secret secret, String key, Object val) {
        return set(secret, key, Serialization.asJson(val));
    }

    public static String toBase64(String in) {
        return Base64.getEncoder()
            .encodeToString(in.getBytes(StandardCharsets.UTF_8));
    }

    public static String fromBase64(String in) {
        return new String(
            Base64.getDecoder().decode(in.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
    }

    public static String generateConnectorSecretId(String id) {
        String answer = id;

        if (!answer.startsWith(Resources.CONNECTOR_PREFIX)) {
            answer = Resources.CONNECTOR_PREFIX + answer;
        }
        if (!answer.endsWith(Resources.CONNECTOR_SECRET_DEPLOYMENT_SUFFIX)) {
            answer += Resources.CONNECTOR_SECRET_DEPLOYMENT_SUFFIX;
        }

        return answer;
    }

    public static String generateSecretId(String id) {
        String answer = id;

        if (!answer.startsWith(Resources.CONNECTOR_PREFIX)) {
            answer = Resources.CONNECTOR_PREFIX + answer;
        }
        if (!answer.endsWith(Resources.CONNECTOR_SECRET_SUFFIX)) {
            answer += Resources.CONNECTOR_SECRET_SUFFIX;
        }

        return answer;
    }
}
