package org.bf2.cos.fleetshard.operator.it.support.assertions;

import org.bf2.cos.fleetshard.api.ManagedConnector;

public final class Assertions extends org.assertj.core.api.Assertions {
    private Assertions() {
    }

    public static ManagedConnectorAssert assertThat(ManagedConnector actual) {
        return new ManagedConnectorAssert(actual);
    }
}
