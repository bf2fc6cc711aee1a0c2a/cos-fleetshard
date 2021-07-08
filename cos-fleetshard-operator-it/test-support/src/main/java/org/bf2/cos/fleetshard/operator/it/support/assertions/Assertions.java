package org.bf2.cos.fleetshard.operator.it.support.assertions;

import org.bf2.cos.fleetshard.api.ManagedConnector;

public final class Assertions {
    private Assertions() {
    }

    public static ManagedConnectorAssert assertThat(ManagedConnector actual) {
        return new ManagedConnectorAssert(actual);
    }
}
