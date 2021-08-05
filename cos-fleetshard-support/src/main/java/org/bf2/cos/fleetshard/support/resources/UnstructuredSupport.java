package org.bf2.cos.fleetshard.support.resources;

import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.model.Scope;
import org.bf2.cos.fleetshard.api.ResourceRef;

public final class UnstructuredSupport {
    private UnstructuredSupport() {
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(ResourceRef reference) {
        return asCustomResourceDefinitionContext(reference, true);
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(
        ResourceRef reference,
        boolean namespaced) {

        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        if (namespaced) {
            builder.withScope(Scope.NAMESPACED.value());
        }

        if (reference.getApiVersion() != null) {
            String[] items = reference.getApiVersion().split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }

        if (reference.getKind() != null) {
            builder.withKind(reference.getKind());
            builder.withPlural(Pluralize.toPlural(reference.getKind().toLowerCase(Locale.US)));
        }

        return builder.build();
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

        return builder.build();
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(
        String apiVersion,
        String kind) {
        return asCustomResourceDefinitionContext(apiVersion, kind, true);
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(
        String apiVersion,
        String kind,
        boolean namespaced) {

        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        if (namespaced) {
            builder.withScope(Scope.NAMESPACED.value());
        }

        if (apiVersion != null) {
            String[] items = apiVersion.split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }
        if (kind != null) {
            builder.withKind(kind);
            builder.withPlural(Pluralize.toPlural(kind.toLowerCase(Locale.US)));
        }

        return builder.build();
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(
        HasMetadata resource) {
        return asCustomResourceDefinitionContext(resource, true);
    }

    public static CustomResourceDefinitionContext asCustomResourceDefinitionContext(
        HasMetadata resource,
        boolean namespaced) {

        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        if (namespaced) {
            builder.withScope(Scope.NAMESPACED.value());
        }

        if (resource.getApiVersion() != null) {
            String[] items = resource.getApiVersion().split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }
        if (resource.getKind() != null) {
            builder.withKind(resource.getKind());
            builder.withPlural(Pluralize.toPlural(resource.getKind().toLowerCase(Locale.US)));
        }

        return builder.build();
    }

    public static ObjectNode getPropertiesAsNode(GenericKubernetesResource resource) {
        return Serialization.jsonMapper().convertValue(resource.getAdditionalProperties(), ObjectNode.class);
    }

    public static JsonNode getPropertyAsNode(GenericKubernetesResource resource, String propertyName) {
        Object property = resource.getAdditionalProperties().get(propertyName);
        if (property == null) {
            return MissingNode.getInstance();
        }
        return Serialization.jsonMapper().convertValue(property, ObjectNode.class);
    }

    public static <T> Optional<T> getPropertyAs(GenericKubernetesResource resource, String propertyName, Class<T> type) {
        Object property = resource.getAdditionalProperties().get(propertyName);
        if (property == null) {
            return Optional.empty();
        }

        return Optional.of(Serialization.jsonMapper().convertValue(property, type));
    }

}