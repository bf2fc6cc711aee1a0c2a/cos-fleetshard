package org.bf2.cos.fleetshard.support.resources;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleetshard.api.ResourceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;

@ApplicationScoped
public class UnstructuredClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnstructuredClient.class);

    private final KubernetesClient kubernetesClient;

    public UnstructuredClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public GenericKubernetesResource get(ResourceRef ref) {
        return get(
            ref.getNamespace(),
            ref.getName(),
            UnstructuredSupport.asResourceDefinitionContext(ref));
    }

    public GenericKubernetesResource get(String namespace, ResourceRef ref) {
        return get(
            namespace,
            ref.getName(),
            UnstructuredSupport.asResourceDefinitionContext(ref));
    }

    public GenericKubernetesResource get(String namespace, String apiVersion, String kind, String name) {
        return get(
            namespace,
            name,
            UnstructuredSupport.asResourceDefinitionContext(apiVersion, kind));
    }

    public GenericKubernetesResource get(String namespace, String name, ResourceDefinitionContext ctx) {
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

    public boolean delete(String namespace, ResourceRef ref) {
        return kubernetesClient
            .genericKubernetesResources(UnstructuredSupport.asResourceDefinitionContext(ref))
            .inNamespace(namespace)
            .withName(ref.getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public GenericKubernetesResource createOrReplace(
        String namespace,
        HasMetadata resource) {

        final GenericKubernetesResource generic;

        if (resource instanceof GenericKubernetesResource) {
            generic = (GenericKubernetesResource) resource;
        } else {
            generic = Serialization.jsonMapper().convertValue(resource, GenericKubernetesResource.class);
        }

        return createOrReplace(namespace, generic);
    }

    public GenericKubernetesResource createOrReplace(
        String namespace,
        GenericKubernetesResource unstructured) {

        return kubernetesClient
            .genericKubernetesResources(UnstructuredSupport.asResourceDefinitionContext(unstructured))
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
            UnstructuredSupport.asResourceDefinitionContext(ref),
            labels,
            watcher);
    }

    private Watch watch(
        String namespace,
        ResourceDefinitionContext ctx,
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
