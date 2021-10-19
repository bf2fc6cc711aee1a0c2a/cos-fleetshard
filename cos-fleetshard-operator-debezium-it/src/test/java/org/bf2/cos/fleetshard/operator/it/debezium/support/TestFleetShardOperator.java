package org.bf2.cos.fleetshard.operator.it.debezium.support;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class TestFleetShardOperator extends FleetShardOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFleetShardOperator.class);

    @Inject
    KubernetesClient client;
    @Inject
    FleetShardOperatorConfig config;

    @ConfigProperty(name = "test.namespace.delete", defaultValue = "true")
    boolean namespaceDelete;

    @Override
    public void start() {
        LOGGER.info("Creating namespace {}", config.connectors().namespace());

        client.namespaces().create(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(config.connectors().namespace())
                .endMetadata()
                .build());

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (namespaceDelete) {
            LOGGER.info("Deleting namespace {}", config.connectors().namespace());

            client.namespaces()
                .withName(config.connectors().namespace())
                .delete();
        }
    }
}
