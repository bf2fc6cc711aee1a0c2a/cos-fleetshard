package org.bf2.cos.fleetshard.support.metrics;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class ResourceAwareMetricsRecorder {
    private final MetricsRecorder recorder;
    private final MetricsRecorderConfig config;

    private ResourceAwareMetricsRecorder(MetricsRecorder recorder, MetricsRecorderConfig config) {
        this.recorder = recorder;
        this.config = config;
    }

    public MetricsRecorder recorder() {
        return recorder;
    }

    public void record(HasMetadata resource, Runnable action) {
        recorder.record(
            action,
            tags(resource, Tags.empty()));
    }

    public void record(HasMetadata resource, Runnable action, String subId) {
        recorder.record(
            action,
            subId,
            tags(resource, Tags.empty()));
    }

    public void record(HasMetadata resource, Runnable action, Consumer<Exception> exceptionHandler) {
        recorder.record(
            action,
            tags(resource, Tags.empty()),
            exceptionHandler);
    }

    public void record(HasMetadata resource, Runnable action, Iterable<Tag> additionalTags,
        Consumer<Exception> exceptionHandler) {
        recorder.record(
            action,
            tags(resource, additionalTags),
            exceptionHandler);

    }

    public void record(HasMetadata resource, Runnable action, Iterable<Tag> additionalTags) {
        recorder.record(
            action,
            tags(resource, additionalTags));
    }

    public void record(HasMetadata resource, Runnable action, String subId, Iterable<Tag> additionalTags) {
        recorder.record(
            action,
            subId,
            tags(resource, additionalTags));
    }

    public void record(HasMetadata resource, Runnable action, String subId, Iterable<Tag> additionalTags,
        Consumer<Exception> exceptionHandler) {
        recorder.record(
            action,
            subId,
            tags(resource, additionalTags),
            exceptionHandler);

    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action) {
        return recorder.recordCallable(
            action,
            tags(resource, Tags.empty()));
    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action, String subId) {
        return recorder.recordCallable(
            action,
            subId,
            tags(resource, Tags.empty()));
    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action, Iterable<Tag> additionalTags) {
        return recorder.recordCallable(
            action,
            tags(resource, additionalTags));
    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action, String subId, Iterable<Tag> additionalTags) {
        return recorder.recordCallable(
            action,
            subId,
            tags(resource, Tags.empty()));
    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action, Consumer<Exception> exceptionHandler) {
        return recorder.recordCallable(
            action,
            "",
            tags(resource, Tags.empty()),
            exceptionHandler);
    }

    public <T> T recordCallable(HasMetadata resource, Callable<T> action, String subId, Iterable<Tag> additionalTags,
        Consumer<Exception> exceptionHandler) {
        return recorder.recordCallable(
            action,
            subId,
            tags(resource, additionalTags),
            exceptionHandler);
    }

    private List<Tag> tags(HasMetadata resource, Iterable<Tag> additionalTags) {
        List<Tag> tags = MetricsSupport.tags(config, resource);
        additionalTags.forEach(tags::add);

        return tags;
    }

    public static ResourceAwareMetricsRecorder of(MetricsRecorder recorder, MetricsRecorderConfig config) {
        return new ResourceAwareMetricsRecorder(recorder, config);
    }

    public static ResourceAwareMetricsRecorder of(MetricsRecorderConfig config, MeterRegistry registry, String id) {
        return new ResourceAwareMetricsRecorder(MetricsRecorder.of(registry, id), config);
    }

    public static ResourceAwareMetricsRecorder of(MetricsRecorderConfig config, MeterRegistry registry, String id,
        List<Tag> tags) {
        return new ResourceAwareMetricsRecorder(MetricsRecorder.of(registry, id, tags), config);
    }
}
