package org.bf2.cos.fleetshard.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.ResourceRefBuilder;

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

    public static ResourceRef asResourceRef(HasMetadata resource) {
        return new ResourceRefBuilder()
                .withApiVersion(resource.getApiVersion())
                .withKind(resource.getKind())
                .withName(resource.getMetadata().getName())
                .build();
    }

    public static ResourceRef asResourceRef(Map<String, Object> resource) {
        Map<String, Object> meta = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());

        return new ResourceRefBuilder()
                .withApiVersion((String) resource.get("apiVersion"))
                .withKind((String) resource.get("kind"))
                .withName((String) meta.get("name"))
                .build();
    }

    public static ObjectReference objectRef(HasMetadata resource) {
        return new ObjectReferenceBuilder()
                .withNamespace(resource.getMetadata().getNamespace())
                .withApiVersion(resource.getApiVersion())
                .withKind(resource.getKind())
                .withName(resource.getMetadata().getName())
                .withUid(resource.getMetadata().getUid())
                .withResourceVersion(resource.getMetadata().getResourceVersion())
                .build();
    }

    public static ObjectReference objectRef(Map<String, Object> resource) {
        Map<String, Object> meta = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());

        return new ObjectReferenceBuilder()
                .withNamespace((String) meta.get("namespace"))
                .withApiVersion((String) resource.get("apiVersion"))
                .withKind((String) resource.get("kind"))
                .withName((String) meta.get("name"))
                .withUid((String) meta.get("uuid"))
                .withResourceVersion((String) meta.get("resourceVersion"))
                .build();

    }

    public static String ownerUid(HasMetadata resource) {
        return resource.getMetadata().getOwnerReferences() != null && !resource.getMetadata().getOwnerReferences().isEmpty()
                ? resource.getMetadata()
                        .getOwnerReferences()
                        .get(0)
                        .getUid()
                : null;
    }

    public static <T extends HasMetadata> T addOwnerReferences(T target, HasMetadata owner) {
        addOwnerReferences(target.getMetadata(), owner);
        return target;
    }

    public static void addOwnerReferences(ObjectMeta target, HasMetadata owner) {
        List<OwnerReference> references = target.getOwnerReferences();
        if (references.stream().noneMatch(r -> Objects.equals(r.getUid(), owner.getMetadata().getUid()))) {
            references.add(asOwnerReference(owner));
        }
    }

    public static <T extends HasMetadata> T removeOwnerReferences(T target, HasMetadata owner) {
        removeOwnerReferences(target.getMetadata(), owner);
        return target;
    }

    public static void removeOwnerReferences(ObjectMeta target, HasMetadata owner) {
        OwnerReference ownerRef = asOwnerReference(owner);
        target.getOwnerReferences().remove(ownerRef);
    }

    public static OwnerReference asOwnerReference(HasMetadata owner) {
        return new OwnerReferenceBuilder()
                .withApiVersion(owner.getApiVersion())
                .withController(false)
                .withKind(owner.getKind())
                .withName(owner.getMetadata().getName())
                .withUid(owner.getMetadata().getUid())
                .build();
    }
}