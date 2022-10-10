package org.bf2.cos.fleetshard.support.metrics;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.bf2.cos.fleetshard.support.exceptions.WrappedRuntimeException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class StaticMetricsRecorder {
    private final MeterRegistry registry;
    private final String id;
    private final List<Tag> tags;

    private final Timer timer;
    private final Counter counter;

    private StaticMetricsRecorder(MeterRegistry registry, String id, List<Tag> tags) {
        this.registry = registry;
        this.id = id;
        this.tags = tags;

        this.timer = Timer.builder(id + ".time")
            .tags(tags)
            .register(registry);
        this.counter = Counter.builder(id + ".count")
            .tags(tags)
            .register(registry);
    }

    public void record(Runnable action) {
        record(
            action,
            e -> {
                throw new WrappedRuntimeException(
                    "Failure recording method execution (id: " + id + ")",
                    e);
            });
    }

    public void record(Runnable action, Consumer<Exception> exceptionHandler) {
        try {
            timer.record(action);
            counter.increment();
        } catch (Exception e) {
            Counter.builder(id + ".count.failure")
                .tags(tags)
                .tag("exception", e.getClass().getName())
                .register(registry)
                .increment();

            exceptionHandler.accept(e);
        }
    }

    public <T> T recordCallable(Callable<T> action) {
        return recordCallable(
            action,
            e -> {
                throw new WrappedRuntimeException(
                    "Failure recording method execution (id: " + id + ")",
                    e);
            });
    }

    public <T> T recordCallable(Callable<T> action, Consumer<Exception> exceptionHandler) {
        try {
            var answer = timer.recordCallable(action);
            counter.increment();

            return answer;
        } catch (Exception e) {
            Counter.builder(id + ".count.failure")
                .tags(tags)
                .tag("exception", e.getClass().getName())
                .register(registry)
                .increment();

            exceptionHandler.accept(e);
        }

        return null;
    }

    public static StaticMetricsRecorder of(MeterRegistry registry, String id) {
        return new StaticMetricsRecorder(registry, id, Collections.emptyList());
    }

    public static StaticMetricsRecorder of(MeterRegistry registry, String id, List<Tag> tags) {
        return new StaticMetricsRecorder(registry, id, tags);
    }
}
