package org.bf2.cos.fleetshard.operator.camel;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.operand.OperandController;
import org.bf2.cos.fleetshard.operator.operand.OperandControllerMetricsWrapper;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.instrument.MeterRegistry;

public class CamelProducers {
    @Singleton
    @Produces
    public ManagedConnectorOperator operator(FleetShardOperatorConfig config) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(config.operator().id())
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, CamelConstants.OPERATOR_TYPE)
                .addToLabels(Resources.LABEL_OPERATOR_VERSION, config.operator().version())
                .addToLabels(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_OPERATOR)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withVersion(config.operator().version())
                .withType(CamelConstants.OPERATOR_TYPE)
                .withRuntime(CamelConstants.OPERATOR_RUNTIME)
                .build())
            .build();
    }

    @Singleton
    @Produces
    public OperandController operandController(
        FleetShardOperatorConfig fleetShardOperatorConfig,
        MeterRegistry registry,
        KubernetesClient kubernetesClient,
        CamelOperandConfiguration operandConfig) {

        return new OperandControllerMetricsWrapper(
            new CamelOperandController(
                fleetShardOperatorConfig,
                kubernetesClient,
                operandConfig),
            ResourceAwareMetricsRecorder.of(
                fleetShardOperatorConfig.metrics().recorder(),
                registry,
                fleetShardOperatorConfig.metrics().baseName() + ".controller.event.operators.operand"));
    }
}
