package org.bf2.cos.fleetshard.operator.it.support.assertions;

import org.assertj.core.api.AbstractAssert;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;

public class ManagedConnectorAssert extends AbstractAssert<ManagedConnectorAssert, ManagedConnector> {
    public ManagedConnectorAssert(ManagedConnector actual) {
        super(actual, ManagedConnectorAssert.class);
    }

    public ManagedConnectorAssert hasStatus() {
        isNotNull();

        if (actual.getStatus() == null) {
            failWithMessage(
                "Expected managed connector status not be null");
        }

        return this;
    }

    public ManagedConnectorAssert hasResources() {
        isNotNull();
        hasStatus();

        if (actual.getStatus().getResources() == null) {
            failWithMessage(
                "Expected managed connector to have resources");
        }

        return this;
    }

    public ManagedConnectorAssert isInPhase(ManagedConnectorStatus.PhaseType phase) {
        isNotNull();
        hasStatus();

        if (!actual.getStatus().isInPhase(phase)) {
            failWithMessage(
                "Expected managed connector to have phase %s but was %s",
                phase.name(),
                actual.getStatus().getPhase().name());
        }

        return this;
    }
}
