package org.bf2.cos.fleetshard.support.unstructured;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.support.unstructured.UnstructuredSupport.asCustomResourceDefinitionContext;

@ApplicationScoped
public class UnstructuredClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnstructuredClient.class);

    private final KubernetesClient kubernetesClient;

    public UnstructuredClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public GenericKubernetesResource get(String namespace, ResourceRef ref) {
        return get(
            namespace,
            ref.getName(),
            asCustomResourceDefinitionContext(ref));
    }

    public GenericKubernetesResource get(String namespace, String name, CustomResourceDefinitionContext ctx) {
        try {
            return kubernetesClient
                .genericKubernetesResources(ctx)
                .inNamespace(namespace)
                .withName(name)
                .get();
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }
        return null;
    }

    public ObjectNode getAsNode(String namespace, String apiVersion, String kind, String name) {
        return getAsNode(namespace, name, asCustomResourceDefinitionContext(apiVersion, kind));
    }

    public ObjectNode getAsNode(String namespace, ResourceRef ref) {
        return getAsNode(namespace, ref.getName(), asCustomResourceDefinitionContext(ref));
    }

    public ObjectNode getAsNode(String namespace, String name, CustomResourceDefinitionContext ctx) {
        try {
            GenericKubernetesResource resource = get(namespace, name, ctx);
            if (resource != null) {
                return Serialization.jsonMapper().convertValue(resource, ObjectNode.class);
            }
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }
        return null;
    }

    public boolean delete(String namespace, ResourceRef ref) {
        return kubernetesClient
            .genericKubernetesResources(asCustomResourceDefinitionContext(ref))
            .inNamespace(namespace)
            .withName(ref.getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public GenericKubernetesResource createOrReplace(
        String namespace,
        GenericKubernetesResource unstructured) {

        return kubernetesClient
            .genericKubernetesResources(asCustomResourceDefinitionContext(unstructured))
            .inNamespace(namespace)
            .withName(unstructured.getMetadata().getName())
            .createOrReplace(unstructured);
    }

    public Watch watch(
        String namespace,
        ResourceRef ref,
        Map<String, String> labels,
        Watcher<GenericKubernetesResource> watcher) {

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
        Watcher<GenericKubernetesResource> watcher) {

        LOGGER.info("Watch {}: namespace:{}, labels:{}", ctx, namespace, labels);

        return kubernetesClient
            .genericKubernetesResources(ctx)
            .inNamespace(namespace)
            .withLabels(labels)
            .watch(watcher);
    }
}
