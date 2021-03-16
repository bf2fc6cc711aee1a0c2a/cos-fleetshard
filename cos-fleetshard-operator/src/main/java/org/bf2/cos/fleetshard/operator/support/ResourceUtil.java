package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;

public final class ResourceUtil {
    private ResourceUtil() {
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

    public static String ownerUid(HasMetadata resource) {
        return resource.getMetadata().getOwnerReferences() != null && !resource.getMetadata().getResourceVersion().isEmpty()
                ? resource.getMetadata()
                        .getOwnerReferences()
                        .get(0)
                        .getUid()
                : null;
    }
}