package org.bf2.cos.fleetshard.operator.it.camel.support;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.operator.FleetShardOperator;
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

    @ConfigProperty(name = "test.namespace")
    String namespace;

    @ConfigProperty(name = "test.namespace.delete", defaultValue = "true")
    boolean namespaceDelete;

    @Override
    public void start() {
        LOGGER.info("Creating namespace {}", namespace);

        client.namespaces().create(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .endMetadata()
                .build());

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (namespaceDelete) {
            LOGGER.info("Deleting namespace {}", namespace);

            client.namespaces().withName(namespace).delete();
        }
    }
}
