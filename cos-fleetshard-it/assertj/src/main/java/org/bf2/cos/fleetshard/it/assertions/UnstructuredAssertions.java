package org.bf2.cos.fleetshard.it.assertions;

import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;

public final class UnstructuredAssertions {
    private UnstructuredAssertions() {
    }

    public static UnstructuredClientAssert assertThatUnstructured(UnstructuredClient actual) {
        return new UnstructuredClientAssert(actual);
    }
}
