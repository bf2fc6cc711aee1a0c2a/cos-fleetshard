package org.bf2.cos.fleetshard.common;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Pluralize;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.model.Scope;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UnstructuredClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnstructuredClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    public Map<String, Object> get(String namespace, ResourceRef ref) {
        return get(namespace, asCustomResourceDefinitionContext(ref));
    }

    public JsonNode getAsNode(String namespace, ResourceRef ref) {
        Map<String, Object> unstructured = get(namespace, ref);
        JsonNode answer = Serialization.jsonMapper().valueToTree(unstructured);

        return answer;
    }

    public Map<String, Object> get(String namespace, JsonNode ref) {
        return get(namespace, asCustomResourceDefinitionContext(ref));
    }

    public Map<String, Object> get(String namespace, CustomResourceDefinitionContext ctx) {
        return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .get(namespace, ctx.getName());
    }

    public boolean delete(String namespace, ResourceRef ref) {
        return delete(namespace, asCustomResourceDefinitionContext(ref));
    }

    public boolean delete(String namespace, JsonNode ref) {
        return delete(namespace, asCustomResourceDefinitionContext(ref));
    }

    public boolean delete(String namespace, CustomResourceDefinitionContext ctx) {
        return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .delete(ctx.getName());
    }

    public Map<String, Object> createOrReplace(String namespace, ResourceRef ref, Map<String, Object> unstructured)
            throws IOException {

        return createOrReplace(
                namespace,
                asCustomResourceDefinitionContext(ref),
                unstructured);
    }

    public Map<String, Object> createOrReplace(String namespace, JsonNode ref, Map<String, Object> unstructured)
            throws IOException {

        return createOrReplace(
                namespace,
                asCustomResourceDefinitionContext(ref),
                unstructured);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrReplace(String namespace, JsonNode unstructured)
            throws IOException {

        return createOrReplace(
                namespace,
                asCustomResourceDefinitionContext(unstructured),
                Serialization.jsonMapper().treeToValue(unstructured, Map.class));
    }

    private Map<String, Object> createOrReplace(
            String namespace,
            CustomResourceDefinitionContext ctx,
            Map<String, Object> unstructured)
            throws IOException {

        return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .createOrReplace(Serialization.asJson(unstructured));
    }

    public void watch(String namespace, ResourceRef ref, Watcher<String> watcher) {
        watch(
                namespace,
                asCustomResourceDefinitionContext(ref),
                watcher);
    }

    public void watch(String namespace, JsonNode ref, Watcher<String> watcher) {
        watch(
                namespace,
                asCustomResourceDefinitionContext(ref),
                watcher);
    }

    @SuppressWarnings("unchecked")
    private void watch(
            String namespace,
            CustomResourceDefinitionContext ctx,
            Watcher<String> watcher) {

        try {
            kubernetesClient
                    .customResource(ctx)
                    .inNamespace(namespace)
                    .watch(watcher);
        } catch (IOException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }

    // *************************************
    //
    // Helpers
    //
    // *************************************

    private CustomResourceDefinitionContext asCustomResourceDefinitionContext(ResourceRef reference) {
        return asCustomResourceDefinitionContext(reference, true);
    }

    private CustomResourceDefinitionContext asCustomResourceDefinitionContext(
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

    private CustomResourceDefinitionContext asCustomResourceDefinitionContext(JsonNode node) {
        return asCustomResourceDefinitionContext(node, true);
    }

    private CustomResourceDefinitionContext asCustomResourceDefinitionContext(JsonNode node, boolean namespaced) {
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
