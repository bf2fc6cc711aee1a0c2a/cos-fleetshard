package org.bf2.cos.fleetshard.common;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Pluralize;
import io.fabric8.kubernetes.model.Scope;

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
        return resource.getMetadata().getOwnerReferences() != null && !resource.getMetadata().getResourceVersion().isEmpty()
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
            references.add(new OwnerReferenceBuilder()
                    .withApiVersion(owner.getApiVersion())
                    .withController(true)
                    .withKind(owner.getKind())
                    .withName(owner.getMetadata().getName())
                    .withUid(owner.getMetadata().getUid())
                    .build());
        }
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(JsonNode node) {
        return asCustomResourceDefinitionContext(node, true);
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(JsonNode node, boolean namespaced) {
        if (!node.isObject()) {
            throw new IllegalArgumentException("Not an ObjectNode");
        }

        ObjectNode on = (ObjectNode) node;
        JsonNode version = on.at("/apiVersion");
        JsonNode kind = on.at("/kind");
        JsonNode name = on.at("/metadata/name");

        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        if (namespaced) {
            builder.withScope(Scope.NAMESPACED.value());
        }

        if (!version.isMissingNode()) {
            String[] items = version.asText().split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }
        if (!kind.isMissingNode()) {
            builder.withKind(kind.asText());
            builder.withPlural(Pluralize.toPlural(kind.asText().toLowerCase(Locale.US)));
        }
        if (!name.isMissingNode()) {
            builder.withName(name.asText());
        }

        return builder.build();
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(Map<String, Object> node) {
        return asCustomResourceDefinitionContext(node, true);
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(Map<String, Object> node,
            boolean namespaced) {
        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        if (namespaced) {
            builder.withScope(Scope.NAMESPACED.value());
        }

        String version = (String) node.get("apiVersion");
        if (version != null) {
            String[] items = version.split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }

        String kind = (String) node.get("kind");
        if (kind != null) {
            builder.withKind(kind);
            builder.withPlural(Pluralize.toPlural(kind.toLowerCase(Locale.US)));
        }

        Map<String, Object> meta = (Map<String, Object>) node.get("metadata");
        if (meta != null) {
            String name = (String) meta.get("name");
            if (name != null) {
                builder.withName(name);
            }
        }

        return builder.build();
    }
}