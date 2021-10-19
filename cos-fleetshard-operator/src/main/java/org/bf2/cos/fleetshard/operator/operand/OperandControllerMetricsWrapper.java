package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.support.MetricsRecorder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

public class OperandControllerMetricsWrapper implements OperandController {
    private OperandController wrappedOperandController;
    private MetricsRecorder metricsRecorder;

    public OperandControllerMetricsWrapper(OperandController wrappedOperandController, MetricsRecorder metricsRecorder) {
        this.wrappedOperandController = wrappedOperandController;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return metricsRecorder.recordCallable(
            () -> wrappedOperandController.getResourceTypes(), ".getResourceTypes");
    }

    @Override
    public List<HasMetadata> reify(ManagedConnector connector, Secret secret) {
        return metricsRecorder.recordCallable(
            () -> wrappedOperandController.reify(connector, secret), ".reify");
    }

    @Override
    public void status(ManagedConnector connector) {
        metricsRecorder.record(
            () -> wrappedOperandController.status(connector), ".status");
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return metricsRecorder.recordCallable(
            () -> wrappedOperandController.stop(connector), ".stop");
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        return metricsRecorder.recordCallable(
            () -> wrappedOperandController.delete(connector), ".delete");
    }
}
