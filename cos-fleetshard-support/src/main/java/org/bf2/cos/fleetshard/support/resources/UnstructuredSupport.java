package org.bf2.cos.fleetshard.support.resources;

import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;

public final class UnstructuredSupport {
    private UnstructuredSupport() {
    }

    public static ResourceDefinitionContext asResourceDefinitionContext(ResourceRef reference) {
        return asResourceDefinitionContext(
            reference.getApiVersion(),
            reference.getKind());
    }

    public static ResourceDefinitionContext asResourceDefinitionContext(JsonNode node) {
        return asResourceDefinitionContext(
            node.requiredAt("/apiVersion").asText(),
            node.requiredAt("/kind").asText());
    }

    public static ResourceDefinitionContext asResourceDefinitionContext(HasMetadata resource) {
        return asResourceDefinitionContext(
            resource.getApiVersion(),
            resource.getKind());
    }

    public static ResourceDefinitionContext asResourceDefinitionContext(String apiVersion, String kind) {
        ResourceDefinitionContext.Builder builder = new ResourceDefinitionContext.Builder();
        builder.withNamespaced(true);

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

    public static <T> Optional<T> getPropertyAs(
        GenericKubernetesResource resource,
        String propertyName,
        Class<T> type) {

        Object property = resource.getAdditionalProperties().get(propertyName);
        if (property == null) {
            return Optional.empty();
        }

        return Optional.of(Serialization.jsonMapper().convertValue(property, type));
    }

}