package org.bf2.cos.fleetshard.operator.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;

import static org.bf2.cos.fleetshard.operator.it.support.ResourceUtil.asCustomResourceDefinitionContext;

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
}
