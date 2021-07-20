package org.bf2.cos.fleetshard.support.unstructured;

import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
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

        if (reference.getName() != null) {
            builder.withName(reference.getName());
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
}