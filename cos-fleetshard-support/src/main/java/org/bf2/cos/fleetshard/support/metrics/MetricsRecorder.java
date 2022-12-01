package org.bf2.cos.fleetshard.support.metrics;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.bf2.cos.fleetshard.support.exceptions.WrappedRuntimeException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public class MetricsRecorder {
    private final MeterRegistry registry;
    private final String id;
    private final List<Tag> tags;

    private MetricsRecorder(MeterRegistry registry, String id, List<Tag> tags) {
        this.registry = registry;
        this.id = id;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void record(Runnable action) {
        record(action, "", Tags.empty());
    }

    public void record(Runnable action, String subId) {
        record(action, subId, Tags.empty());
    }

    public void record(Runnable action, Consumer<Exception> exceptionHandler) {
        record(action, "", Tags.empty(), exceptionHandler);
    }

    public void record(Runnable action, Iterable<Tag> additionalTags, Consumer<Exception> exceptionHandler) {
        record(action, "", additionalTags, exceptionHandler);
    }

    public void record(Runnable action, Iterable<Tag> additionalTags) {
        record(action, "", additionalTags);
    }

    public void record(Runnable action, String subId, Iterable<Tag> additionalTags) {
        record(
            action,
            subId,
            additionalTags,
            e -> {
                throw new WrappedRuntimeException(
                    "Failure recording method execution (id: " + id + subId + ")",
                    e);
            });
    }

    public void record(Runnable action, String subId, Iterable<Tag> additionalTags, Consumer<Exception> exceptionHandler) {
        try {
            Timer.builder(id + subId + ".time")
                .tags(tags)
                .tags(additionalTags)
                .register(registry)
                .record(action);

            Counter.builder(id + subId + ".count")
                .tags(tags)
                .tags(additionalTags)
                .register(registry)
                .increment();
        } catch (Exception e) {
            Counter.builder(id + subId + ".count.failure")
                .tags(tags)
                .tags(additionalTags)
                .tag("exception", e.getClass().getName())
                .register(registry)
                .increment();

            exceptionHandler.accept(e);
        }
    }

    public <T> T recordCallable(Callable<T> action) {
        return recordCallable(action, "", Tags.empty());
    }

    public <T> T recordCallable(Callable<T> action, String subId) {
        return recordCallable(action, subId, Tags.empty());
    }

    public <T> T recordCallable(Callable<T> action, Iterable<Tag> additionalTags) {
        return recordCallable(action, "", additionalTags);
    }

    public <T> T recordCallable(Callable<T> action, String subId, Iterable<Tag> additionalTags) {
        return recordCallable(
            action,
            subId,
            additionalTags,
            e -> {
                throw new WrappedRuntimeException(
                    "Failure recording method execution (id: " + id + subId + ")",
                    e);
            });
    }

    public <T> T recordCallable(Callable<T> action, Consumer<Exception> exceptionHandler) {
        return recordCallable(action, "", Tags.empty(), exceptionHandler);
    }

    public <T> T recordCallable(Callable<T> action, String subId, Iterable<Tag> additionalTags,
        Consumer<Exception> exceptionHandler) {
        try {
            var answer = Timer.builder(id + subId + ".time")
                .tags(tags)
                .tags(additionalTags)
                .publishPercentiles(0.3, 0.5, 0.95)
                .publishPercentileHistogram()
                .register(registry)
                .recordCallable(action);

            Counter.builder(id + subId + ".count")
                .tags(tags)
                .tags(additionalTags)
                .register(registry)
                .increment();

            return answer;
        } catch (Exception e) {
            Counter.builder(id + subId + ".count.failure")
                .tags(tags)
                .tags(additionalTags)
                .tag("exception", e.getClass().getName())
                .register(registry)
                .increment();

            exceptionHandler.accept(e);
        }

        return null;
    }

    public static MetricsRecorder of(MeterRegistry registry, String id) {
        return new MetricsRecorder(registry, id, Collections.emptyList());
    }

    public static MetricsRecorder of(MeterRegistry registry, String id, List<Tag> tags) {
        return new MetricsRecorder(registry, id, tags);
    }
}
