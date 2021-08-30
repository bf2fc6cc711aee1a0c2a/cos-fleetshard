package org.bf2.cos.fleetshard.it.assertions;

import java.util.function.Consumer;

import org.assertj.core.api.AbstractAssert;
import org.bf2.cos.fleetshard.support.function.Functions;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;

public class UnstructuredClientAssert extends AbstractAssert<UnstructuredClientAssert, UnstructuredClient> {
    public UnstructuredClientAssert(UnstructuredClient actual) {
        super(actual, UnstructuredClientAssert.class);
    }

    public UnstructuredClientAssert doesNotHaveResource(String namespace, String apiVersion, String kind, String name) {
        isNotNull();

        GenericKubernetesResource resource = actual.get(namespace, apiVersion, kind, name);
        if (resource != null) {
            failWithMessage(
                "Expected resources %s:%s:%s to not exists in namespace %s",
                apiVersion,
                kind,
                name,
                namespace);
        }

        return this;
    }

    public UnstructuredClientAssert hasResource(String namespace, String apiVersion, String kind, String name) {
        return hasResourceSatisfying(namespace, apiVersion, kind, name, Functions.noOpConsumer());
    }

    public UnstructuredClientAssert hasResourceSatisfying(
        String namespace,
        String apiVersion,
        String kind,
        String name,
        Consumer<GenericKubernetesResource> consumer) {

        isNotNull();

        GenericKubernetesResource resource = actual.get(namespace, apiVersion, kind, name);
        if (resource == null) {
            failWithMessage(
                "Expected resources %s:%s:%s to exists in namespace %s",
                apiVersion,
                kind,
                name,
                namespace);
        } else {
            consumer.accept(resource);
        }

        return this;
    }

    public UnstructuredClientAssert doesNotHaveSecret(String namespace, String name) {
        return doesNotHaveResource(
            namespace,
            "v1",
            "Secret",
            name);
    }

    public UnstructuredClientAssert hasSecret(String namespace, String name) {
        return hasResource(
            namespace,
            "v1",
            "Secret",
            name);
    }

    public UnstructuredClientAssert hasSecretSatisfying(String namespace, String name,
        Consumer<GenericKubernetesResource> consumer) {
        return hasResourceSatisfying(
            namespace,
            "v1",
            "Secret",
            name,
            consumer);
    }

    public UnstructuredClientAssert doesNotKameletBinding(String namespace, String name) {
        return doesNotHaveResource(
            namespace,
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            name);
    }

    public UnstructuredClientAssert hasKameletBinding(String namespace, String name) {
        return hasResource(
            namespace,
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            name);
    }

    public UnstructuredClientAssert hasKameletBindingSatisfying(String namespace, String name,
        Consumer<GenericKubernetesResource> consumer) {
        return hasResourceSatisfying(
            namespace,
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            name,
            consumer);
    }
}
