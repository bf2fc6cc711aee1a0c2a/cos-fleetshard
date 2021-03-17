package org.bf2.cos.fleetshard.operator.support;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;

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

    public static <T extends HasMetadata> T setOwnerReferences(T target, HasMetadata owner) {
        setOwnerReferences(target.getMetadata(), owner);
        return target;
    }

    public static void setOwnerReferences(ObjectMeta target, HasMetadata owner) {
        target.setOwnerReferences(
                List.of(
                        new OwnerReferenceBuilder()
                                .withApiVersion(owner.getApiVersion())
                                .withController(true)
                                .withKind(owner.getKind())
                                .withName(owner.getMetadata().getName())
                                .withUid(owner.getMetadata().getUid())
                                .build()));
    }
}