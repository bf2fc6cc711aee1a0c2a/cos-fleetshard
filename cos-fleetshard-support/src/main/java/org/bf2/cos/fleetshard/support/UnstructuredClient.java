package org.bf2.cos.fleetshard.support;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UnstructuredClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnstructuredClient.class);

    private final KubernetesClient kubernetesClient;

    public UnstructuredClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public Map<String, Object> get(String namespace, ResourceRef ref) {
        return get(
            namespace,
            ResourceUtil.asCustomResourceDefinitionContext(ref));
    }

    public JsonNode getAsNode(String namespace, ResourceRef ref) {
        Map<String, Object> unstructured = get(namespace, ref);
        if (unstructured == null) {
            return null;
        }

        return Serialization.jsonMapper().valueToTree(unstructured);
    }

    public JsonNode getAsNode(String namespace, String apiVersion, String kind, String name) {
        return getAsNode(namespace, new ResourceRef(apiVersion, kind, name));
    }

    public Map<String, Object> get(String namespace, JsonNode ref) {
        return get(namespace, ResourceUtil.asCustomResourceDefinitionContext(ref));
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
        return delete(namespace, ResourceUtil.asCustomResourceDefinitionContext(ref));
    }

    public boolean delete(String namespace, CustomResourceDefinitionContext ctx) throws IOException {
        return kubernetesClient
            .customResource(ctx)
            .inNamespace(namespace)
            .delete(namespace, ctx.getName());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrReplace(String namespace, JsonNode unstructured)
        throws IOException {

        return createOrReplace(
            namespace,
            ResourceUtil.asCustomResourceDefinitionContext(unstructured),
            Serialization.jsonMapper().treeToValue(unstructured, Map.class));
    }

    private Map<String, Object> createOrReplace(
        String namespace,
        CustomResourceDefinitionContext ctx,
        Map<String, Object> unstructured)
        throws IOException {

        setResourceVersion(namespace, ctx, unstructured);

        return kubernetesClient
            .customResource(ctx)
            .inNamespace(namespace)
            .createOrReplace(namespace, unstructured);
    }

    @SuppressWarnings("unchecked")
    private void setResourceVersion(
        String namespace,
        CustomResourceDefinitionContext ctx,
        Map<String, Object> unstructured) {

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
    }

    public Watch watch(String namespace, ResourceRef ref, Map<String, String> labels, Watcher<String> watcher) {
        return watch(
            namespace,
            ResourceUtil.asCustomResourceDefinitionContext(ref),
            labels,
            watcher);
    }

    private Watch watch(
        String namespace,
        CustomResourceDefinitionContext ctx,
        Map<String, String> labels,
        Watcher<String> watcher) {

        LOGGER.info("Watch {}: namespace:{}, labels:{}", ctx, namespace, labels);

        try {
            return kubernetesClient
                .customResource(ctx)
                .inNamespace(namespace)
                .watch(namespace, null, labels, new ListOptionsBuilder().build(), watcher);
        } catch (IOException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }
}
