package org.bf2.cos.fleetshard.operator.debezium;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.support.resources.Resources;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class DebeziumProducers {
    @Singleton
    @Produces
    public ManagedConnectorOperator operator(
        FleetShardOperatorConfig config) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(config.operator().id())
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, DebeziumConstants.OPERATOR_TYPE)
                .addToLabels(Resources.LABEL_OPERATOR_VERSION, config.operator().version())
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withVersion(config.operator().version())
                .withType(DebeziumConstants.OPERATOR_TYPE)
                .withRuntime(DebeziumConstants.OPERATOR_RUNTIME)
                .build())
            .build();
    }
}
