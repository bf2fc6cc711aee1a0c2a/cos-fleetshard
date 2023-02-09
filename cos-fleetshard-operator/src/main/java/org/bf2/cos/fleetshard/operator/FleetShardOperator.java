package org.bf2.cos.fleetshard.operator;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfigMapWatcher;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.ResourceAwareMetricsRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

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
    @Inject
    EventClient eventClient;
    @Inject
    MeterRegistry registry;

    private ConnectorConfigMapWatcher configMapWatcher;

    public void start() {
        LOGGER.info("Starting operator (id: {}, type: {}, version: {})",
            managedConnectorOperator.getMetadata().getName(),
            managedConnectorOperator.getSpec().getType(),
            managedConnectorOperator.getSpec().getVersion());

        client.resource(managedConnectorOperator)
            .inNamespace(config.namespace())
            .createOrReplace();

        List<Tag> tags = List.of(
            Tag.of("cos.operator.id", managedConnectorOperator.getMetadata().getName()),
            Tag.of("cos.operator.type", managedConnectorOperator.getSpec().getType()),
            Tag.of("cos.operator.version", managedConnectorOperator.getSpec().getVersion()));

        this.configMapWatcher = new ConnectorConfigMapWatcher(
            client,
            managedConnectorOperator,
            ResourceAwareMetricsRecorder.of(
                config.metrics().recorder(),
                registry,
                config.metrics().baseName() + ".controller.event.configmaps",
                tags),
            eventClient);

        configMapWatcher.start();

        operator.start();
    }

    public void stop() {
        LOGGER.info("Stopping operator (id: {}, type: {}, version: {})",
            managedConnectorOperator.getMetadata().getName(),
            managedConnectorOperator.getSpec().getType(),
            managedConnectorOperator.getSpec().getVersion());

        if (configMapWatcher != null) {
            configMapWatcher.close();
        }

        operator.stop();
    }
}
