package org.bf2.cos.fleetshard.operator.e2e.glues;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;

@Singleton
public class Awaiter {
    public volatile long atMost = 30_000L;
    public volatile long pollDelay = 100L;
    public volatile long pollInterval = 500L;

    public void until(Callable<Boolean> conditionEvaluator) {
        Awaitility.await()
            .atMost(atMost, TimeUnit.MILLISECONDS)
            .pollDelay(pollDelay, TimeUnit.MILLISECONDS)
            .pollInterval(pollInterval, TimeUnit.MILLISECONDS)
            .until(conditionEvaluator);
    }

    public void until(Callable<Boolean> conditionEvaluator, Supplier<String> x) {
        Awaitility.await()
            .atMost(atMost, TimeUnit.MILLISECONDS)
            .pollDelay(pollDelay, TimeUnit.MILLISECONDS)
            .pollInterval(pollInterval, TimeUnit.MILLISECONDS)
            .until(conditionEvaluator);
    }

    public void untilAsserted(ThrowingRunnable conditionEvaluator) {
        Awaitility.await()
            .atMost(atMost, TimeUnit.MILLISECONDS)
            .pollDelay(pollDelay, TimeUnit.MILLISECONDS)
            .pollInterval(pollInterval, TimeUnit.MILLISECONDS)
            .untilAsserted(conditionEvaluator);
    }
}
