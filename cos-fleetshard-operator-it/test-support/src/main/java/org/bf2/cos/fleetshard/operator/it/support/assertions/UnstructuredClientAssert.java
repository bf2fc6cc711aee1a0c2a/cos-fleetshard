package org.bf2.cos.fleetshard.operator.it.support.assertions;

import org.assertj.core.api.AbstractAssert;
import org.bf2.cos.fleetshard.support.UnstructuredClient;

public class UnstructuredClientAssert extends AbstractAssert<UnstructuredClientAssert, UnstructuredClient> {
    public UnstructuredClientAssert(UnstructuredClient actual) {
        super(actual, UnstructuredClientAssert.class);
    }

    public UnstructuredClientAssert hasResource(String namespace, String apiVersion, String kind, String name) {
        isNotNull();

        if (actual.getAsNode(namespace, apiVersion, kind, name) == null) {
            failWithMessage(
                "Expected resources %s:%s:%s to exists in namespace %s",
                apiVersion,
                kind,
                name,
                namespace);
        }

        return this;
    }
}
