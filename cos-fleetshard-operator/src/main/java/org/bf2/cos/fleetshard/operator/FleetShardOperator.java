package org.bf2.cos.fleetshard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;

@ApplicationScoped
public class FleetShardOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardOperator.class);

    @Inject
    ManagedConnectorOperator managedConnectorOperator;
    @Inject
    KubernetesClient client;
    @Inject
    Operator operator;
    @Inject
    FleetShardOperatorConfig config;

    public void start() {
        LOGGER.info("Starting operator (id: {}, type: {}, version: {})",
            managedConnectorOperator.getMetadata().getName(),
            managedConnectorOperator.getSpec().getType(),
            managedConnectorOperator.getSpec().getVersion());

        client.resources(ManagedConnectorOperator.class)
            .inNamespace(config.operators().namespace())
            .createOrReplace(managedConnectorOperator);

        operator.start();
    }

    public void stop() {
        LOGGER.info("Stopping operator (id: {}, type: {}, version: {})",
            managedConnectorOperator.getMetadata().getName(),
            managedConnectorOperator.getSpec().getType(),
            managedConnectorOperator.getSpec().getVersion());

        operator.close();
    }
}
