package org.bf2.cos.fleetshard.support;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.ResourceRefBuilder;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_CHECKSUM;
import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_RESOURCE_VERSION;

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

    public static ResourceRef asResourceRef(GenericKubernetesResource resource) {
        return new ResourceRefBuilder()
            .withApiVersion(resource.getApiVersion())
            .withKind(resource.getKind())
            .withName(resource.getMetadata().getName())
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

    public static Optional<String> getDeletionMode(GenericKubernetesResource resource) {
        if (resource.getMetadata() == null) {
            return Optional.empty();
        }

        if (resource.getMetadata().getAnnotations() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(resource.getMetadata().getAnnotations().get(ANNOTATION_DELETION_MODE));
    }

    public static String clusterName(String clusterId) {
        String clusterName = clusterId.toLowerCase(Locale.US);
        clusterName = KubernetesResourceUtil.sanitizeName(clusterName);
        clusterName = Constants.CLUSTER_NAME_PREFIX + clusterName;

        return clusterName;
    }

    public static String generateConnectorId() {
        return "c-" + uid();
    }

    public static String generateSecretId(long resourceVersion) {
        return "s-" + uid() + "-" + resourceVersion;
    }

    public static String uid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static Long getDeploymentResourceVersion(HasMetadata resource) {
        var labels = resource.getMetadata().getLabels();
        if (labels == null) {
            return null;
        }

        String version = labels.get(LABEL_DEPLOYMENT_RESOURCE_VERSION);
        return version != null
            ? Long.parseLong(version)
            : null;
    }
}