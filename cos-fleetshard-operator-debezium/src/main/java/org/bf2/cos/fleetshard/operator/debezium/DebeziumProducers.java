package org.bf2.cos.fleetshard.operator.debezium;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class DebeziumProducers {
    @Singleton
    @Produces
    public ManagedConnectorOperator operator(
        @ConfigProperty(name = "cos.operator.id") String operatorId,
        @ConfigProperty(name = "cos.operator.version") String operatorVersion) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(operatorId)
                .addToLabels(ManagedConnectorOperator.LABEL_OPERATOR_TYPE, DebeziumConstants.OPERATOR_TYPE)
                .addToLabels(ManagedConnectorOperator.LABEL_OPERATOR_VERSION, operatorVersion)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withVersion(operatorVersion)
                .withType(DebeziumConstants.OPERATOR_TYPE)
                .withRuntime(DebeziumConstants.OPERATOR_RUNTIME)
                .build())
            .build();
    }
}
