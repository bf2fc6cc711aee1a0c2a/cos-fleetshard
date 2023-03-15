package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.operand.OperandController;
import org.bf2.cos.fleetshard.operator.operand.OperandControllerMetricsWrapper;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class TestProducers {
    @Produces
    @Singleton
    public ManagedConnectorOperator operator(
        @ConfigProperty(name = "cos.operator.id") String operatorId,
        @ConfigProperty(name = "cos.operator.version") String operatorVersion) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(operatorId)
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, "connector-operator-it")
                .addToLabels(Resources.LABEL_OPERATOR_VERSION, operatorVersion)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withVersion(operatorVersion)
                .withType("connector-operator-it")
                .withRuntime("none")
                .build())
            .build();
    }

    @Produces
    @Singleton
    public OperandController operand(
        FleetShardOperatorConfig fleetShardOperatorConfig,
        MeterRegistry registry) {

        OperandController controller = new OperandController() {
            @Override
            public List<ResourceDefinitionContext> getResourceTypes() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, EventSource> getEventSources() {
                return Collections.emptyMap();
            }

            @Override
            public List<HasMetadata> reify(ManagedConnector connector, Secret secret, ConfigMap configMap) {
                if (secret.getData() != null && secret.getData().containsKey("reify.fail")) {
                    throw new IllegalArgumentException(Secrets.extract(secret, "reify.fail", String.class));
                }

                return Collections.emptyList();
            }

            @Override
            public void status(ManagedConnector connector) {
                connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_READY);
            }

            @Override
            public boolean stop(ManagedConnector connector) {
                return true;
            }

            @Override
            public boolean delete(ManagedConnector connector) {
                return true;
            }
        };

        return new OperandControllerMetricsWrapper(
            controller,
            ResourceAwareMetricsRecorder.of(
                fleetShardOperatorConfig.metrics().recorder(),
                registry,
                fleetShardOperatorConfig.metrics().baseName() + ".controller.event.operators.operand"));
    }
}
