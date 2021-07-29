package org.bf2.cos.fleetshard.it.assertions;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.AbstractAssert;
import org.bf2.cos.fleetshard.support.function.Functions;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;

import com.fasterxml.jackson.databind.JsonNode;

public class UnstructuredClientAssert extends AbstractAssert<UnstructuredClientAssert, UnstructuredClient> {
    public UnstructuredClientAssert(UnstructuredClient actual) {
        super(actual, UnstructuredClientAssert.class);
    }

    public UnstructuredClientAssert doesNotHaveResource(String namespace, String apiVersion, String kind, String name) {
        isNotNull();

        JsonNode node = actual.getAsNode(namespace, apiVersion, kind, name);
        if (node != null) {
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
        Consumer<ObjectNode> consumer) {

        isNotNull();

        JsonNode node = actual.getAsNode(namespace, apiVersion, kind, name);
        if (node == null) {
            failWithMessage(
                "Expected resources %s:%s:%s to exists in namespace %s",
                apiVersion,
                kind,
                name,
                namespace);
        } else if (!node.isObject()) {
            failWithMessage(
                "Expected resources %s:%s:%s to be an Object, got",
                apiVersion,
                kind,
                name,
                node.getNodeType());
        } else {
            consumer.accept((ObjectNode) node);
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

    public UnstructuredClientAssert hasSecretSatisfying(String namespace, String name, Consumer<ObjectNode> consumer) {
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

    public UnstructuredClientAssert hasKameletBindingSatisfying(String namespace, String name, Consumer<ObjectNode> consumer) {
        return hasResourceSatisfying(
            namespace,
            "camel.apache.org/v1alpha1",
            "KameletBinding",
            name,
            consumer);
    }
}
