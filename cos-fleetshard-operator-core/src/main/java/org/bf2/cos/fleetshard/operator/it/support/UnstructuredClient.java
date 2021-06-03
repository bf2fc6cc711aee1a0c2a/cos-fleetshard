package org.bf2.cos.fleetshard.operator.it.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.model.Scope;
import org.bf2.cos.fleetshard.api.ResourceRef;

@ApplicationScoped
public class UnstructuredClient {
    private final KubernetesClient kubernetesClient;

    public UnstructuredClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public Map<String, Object> get(String namespace, ResourceRef ref) {
        return get(
            namespace,
            asCustomResourceDefinitionContext(ref));
    }

    public JsonNode getAsNode(String namespace, ResourceRef ref) {
        Map<String, Object> unstructured = get(namespace, ref);
        if (unstructured == null) {
            return null;
        }

        return Serialization.jsonMapper().valueToTree(unstructured);
    }

    public Map<String, Object> get(String namespace, JsonNode ref) {
        return get(namespace, asCustomResourceDefinitionContext(ref));
    }

    public Map<String, Object> get(String namespace, CustomResourceDefinitionContext ctx) {
        try {
            return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .get(namespace, ctx.getName());
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }
        return null;
    }

    public boolean delete(String namespace, ResourceRef ref) throws IOException {
        return delete(namespace, asCustomResourceDefinitionContext(ref));
    }

    public boolean delete(String namespace, JsonNode ref) throws IOException {
        return delete(namespace, asCustomResourceDefinitionContext(ref));
    }

    public boolean delete(String namespace, CustomResourceDefinitionContext ctx) throws IOException {
        return kubernetesClient
            .customResource(ctx)
            .inNamespace(namespace)
            .delete(namespace, ctx.getName());
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> createOrReplace(
        String namespace,
        CustomResourceDefinitionContext ctx,
        Map<String, Object> unstructured)
        throws IOException {

        try {
            var old = kubernetesClient.customResource(ctx).get(namespace, ctx.getName());
            if (old != null) {
                var meta = (Map<String, Object>) old.get("metadata");
                if (meta != null) {
                    var rv = meta.get("resourceVersion");
                    if (rv != null) {
                        var umeta = (Map<String, Object>) unstructured.computeIfAbsent("metadata", k -> new HashMap<>());
                        umeta.putIfAbsent("resourceVersion", rv);
                    }
                }
            }
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }

        return kubernetesClient
            .customResource(ctx)
            .inNamespace(namespace)
            .createOrReplace(unstructured);
    }

    public Watch watch(String namespace, ResourceRef ref, Watcher<String> watcher) {
        return watch(
            namespace,
            asCustomResourceDefinitionContext(ref),
            null,
            watcher);
    }

    public Watch watch(String namespace, ResourceRef ref, Map<String, String> labels, Watcher<String> watcher) {
        return watch(
            namespace,
            asCustomResourceDefinitionContext(ref),
            labels,
            watcher);
    }

    public Watch watch(String namespace, JsonNode ref, Watcher<String> watcher) {
        return watch(
            namespace,
            asCustomResourceDefinitionContext(ref),
            null,
            watcher);
    }

    public Watch watch(String namespace, JsonNode ref, Map<String, String> labels, Watcher<String> watcher) {
        return watch(
            namespace,
            asCustomResourceDefinitionContext(ref),
            labels,
            watcher);
    }

    private Watch watch(
        String namespace,
        CustomResourceDefinitionContext ctx,
        Map<String, String> labels,
        Watcher<String> watcher) {

        try {
            return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .watch(null, null, labels, new ListOptionsBuilder().build(), watcher);
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
