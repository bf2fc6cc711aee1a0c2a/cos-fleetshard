package org.bf2.cos.fleetshard.operator.service.observability;

import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public class ObservabilityController implements Reconciler<ConfigMap>, EventSourceInitializer<ConfigMap> {

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext context) {
        return List.of();
    }

    @Override
    public UpdateControl<ConfigMap> reconcile(ConfigMap map, Context context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(ConfigMap resource, Context context) {
        return Reconciler.super.cleanup(resource, context);
    }
}
