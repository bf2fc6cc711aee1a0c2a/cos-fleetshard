package org.bf2.cos.fleetshard.support.resources;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.ResourceRefBuilder;
import org.bson.types.ObjectId;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.LABEL_DEPLOYMENT_RESOURCE_VERSION;

public final class ResourceUtil {
    public static final String CONNECTOR_PREFIX = "mctr";

    private ResourceUtil() {
    }

    public static ResourceRef asResourceRef(GenericKubernetesResource resource) {
        return new ResourceRefBuilder()
            .withApiVersion(resource.getApiVersion())
            .withKind(resource.getKind())
            .withName(resource.getMetadata().getName())
            .build();
    }

    public static Optional<String> getDeletionMode(JsonNode node) {
        final JsonNode mode = node.requiredAt("/metadata/annotations").get(ANNOTATION_DELETION_MODE);
        final Optional<String> answer = Optional.ofNullable(mode).map(JsonNode::asText);

        return answer;
    }

    public static Optional<String> getDeletionMode(HasMetadata resource) {
        if (resource.getMetadata() == null) {
            return Optional.empty();
        }

        if (resource.getMetadata().getAnnotations() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(resource.getMetadata().getAnnotations().get(ANNOTATION_DELETION_MODE));
    }

    public static String generateConnectorId() {
        return CONNECTOR_PREFIX + "-" + uid();
    }

    public static String uid() {
        return ObjectId.get().toString();
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