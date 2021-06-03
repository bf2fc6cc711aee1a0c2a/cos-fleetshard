package org.bf2.cos.fleetshard.operator.it.support;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

public final class TestSupport {
    private TestSupport() {
    }

    public static void await(long timeout, TimeUnit unit, Callable<Boolean> condition) {
        Awaitility.await()
            .atMost(timeout, unit)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(condition);
    }
}
