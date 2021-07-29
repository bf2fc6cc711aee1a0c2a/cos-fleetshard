package org.bf2.cos.fleetshard.it.assertions;

import org.bf2.cos.fleetshard.api.ManagedConnector;

public final class ManagedConnectorAssertions {
    private ManagedConnectorAssertions() {
    }

    public static ManagedConnectorAssert assertThatManagedConnector(ManagedConnector actual) {
        return new ManagedConnectorAssert(actual);
    }
}
