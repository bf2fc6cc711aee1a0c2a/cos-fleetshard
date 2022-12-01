package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

public class OperandControllerMetricsWrapper implements OperandController {
    private final OperandController wrappedOperandController;
    private final ResourceAwareMetricsRecorder metricsRecorder;

    public OperandControllerMetricsWrapper(OperandController wrappedOperandController,
        ResourceAwareMetricsRecorder metricsRecorder) {
        this.wrappedOperandController = wrappedOperandController;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return metricsRecorder.recorder().recordCallable(
            wrappedOperandController::getResourceTypes,
            ".getResourceTypes");
    }

    @Override
    public List<HasMetadata> reify(ManagedConnector connector, Secret secret, ConfigMap configMap) {
        return metricsRecorder.recordCallable(
            connector,
            () -> wrappedOperandController.reify(connector, secret, configMap),
            ".reify");
    }

    @Override
    public void status(ManagedConnector connector) {
        metricsRecorder.record(
            connector,
            () -> wrappedOperandController.status(connector),
            ".status");
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return metricsRecorder.recordCallable(
            connector,
            () -> wrappedOperandController.stop(connector),
            ".stop");
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        return metricsRecorder.recordCallable(
            connector,
            () -> wrappedOperandController.delete(connector),
            ".delete");
    }
}
