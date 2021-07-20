package org.bf2.cos.fleetshard.support;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.ResourceRefBuilder;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_CHECKSUM;
import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;

public final class ResourceUtil {
    private ResourceUtil() {
    }

    /**
     * Extract a {@link ObjectMeta} from an unstructured resource.
     *
     * @param  unstructured             the unstructured content
     * @return                          the {@link ObjectMeta}
     * @throws IllegalArgumentException if the unstructured data does not have the metadata field
     */
    public static ObjectMeta getObjectMeta(Map<String, Object> unstructured) {
        Object meta = unstructured.get("metadata");
        if (meta == null) {
            throw new IllegalArgumentException("Invalid unstructured resource: " + unstructured);
        }

        return Serialization.jsonMapper().convertValue(meta, ObjectMeta.class);
    }

    @SuppressWarnings("unchecked")
    public static ResourceRef asResourceRef(Map<String, Object> resource) {
        Map<String, Object> meta = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());

        return new ResourceRefBuilder()
            .withApiVersion((String) resource.get("apiVersion"))
            .withKind((String) resource.get("kind"))
            .withName((String) meta.get("name"))
            .build();
    }

    public static String computeChecksum(JsonNode node) throws JsonProcessingException {
        byte[] bytes = Serialization.jsonMapper().writeValueAsBytes(node);
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        crc32.getValue();

        return Long.toHexString(crc32.getValue());
    }

    public static String getChecksum(JsonNode node) {
        if (node != null) {
            JsonNode annotations = node.at("/metadata/annotations");
            if (!annotations.isMissingNode()) {
                JsonNode checksum = annotations.get(ANNOTATION_CHECKSUM);
                if (checksum != null) {
                    return checksum.asText();
                }
            }
        }

        return null;
    }

    public static Optional<String> getDeletionMode(JsonNode node) {
        final JsonNode mode = node.requiredAt("/metadata/annotations").get(ANNOTATION_DELETION_MODE);
        final Optional<String> answer = Optional.ofNullable(mode).map(JsonNode::asText);

        return answer;
    }
}