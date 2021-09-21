package org.bf2.cos.fleetshard.support.resources;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;

public final class Secrets {
    public static final String SECRET_ENTRY_CONNECTOR = "connector";
    public static final String SECRET_ENTRY_KAFKA = "kafka";
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

        crc32.getValue();

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

        return Serialization.unmarshal(new String(Base64.getDecoder().decode(val)), type);
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

    public static Secret newSecret(
        String name,
        String clusterId,
        String connectorId,
        String deploymentId,
        long deploymentResourceVersion,
        Map<String, String> additionalLabels) {

        return new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .addToLabels(LABEL_CLUSTER_ID, clusterId)
                .addToLabels(LABEL_CONNECTOR_ID, connectorId)
                .addToLabels(LABEL_DEPLOYMENT_ID, deploymentId)
                .addToLabels(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + deploymentResourceVersion)
                .addToLabels(additionalLabels)
                .build())
            .build();
    }

    public static String generateConnectorSecretId(String deploymentId) {
        return Resources.CONNECTOR_PREFIX + deploymentId + Resources.CONNECTOR_SECRET_DEPLOYMENT_SUFFIX;
    }

    public static String generateSecretId(String deploymentId) {
        return Resources.CONNECTOR_PREFIX + deploymentId + Resources.CONNECTOR_SECRET_SUFFIX;
    }
}
