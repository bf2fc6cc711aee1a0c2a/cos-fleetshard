package org.bf2.cos.fleetshard.support.resources;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.api.ResourceRefBuilder;
import org.bson.types.ObjectId;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;

public final class Resources {

    private Resources() {
    }

    public static ResourceRef asRef(GenericKubernetesResource resource) {
        return new ResourceRefBuilder()
            .withApiVersion(resource.getApiVersion())
            .withKind(resource.getKind())
            .withName(resource.getMetadata().getName())
            .build();
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

    public static String uid() {
        return ObjectId.get().toString();
    }
}