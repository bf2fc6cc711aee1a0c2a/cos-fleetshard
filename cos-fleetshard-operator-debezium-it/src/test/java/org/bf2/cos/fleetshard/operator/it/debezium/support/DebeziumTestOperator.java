package org.bf2.cos.fleetshard.operator.it.debezium.support;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.CosFeatureContext;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class DebeziumTestOperator extends FleetShardOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumTestOperator.class);

    @Inject
    KubernetesClient client;
    @Inject
    CosFeatureContext cosFeatureContext;

    @ConfigProperty(name = "test.namespace.delete", defaultValue = "true")
    boolean namespaceDelete;

    @Override
    public void start() {
        LOGGER.info("Creating test namespaces");

        createNamespace(cosFeatureContext.getConnectorsNamespace());
        createNamespace(cosFeatureContext.getOperatorsNamespace());

        super.start();
    }

    @Override
    public void stop() {

        LOGGER.info("Deleting test namespaces");

        if (namespaceDelete) {
            deleteNamespace(cosFeatureContext.getConnectorsNamespace());
            deleteNamespace(cosFeatureContext.getOperatorsNamespace());
        }

        super.stop();
    }

    private void createNamespace(String name) {
        if (client.namespaces().withName(name).get() == null) {
            LOGGER.info("Creating namespace {}", name);

            client.resource(
                new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                    .build())
                .create();
        }
    }

    private void deleteNamespace(String name) {
        if (client.namespaces().withName(name).get() == null) {
            LOGGER.info("Deleting namespace {}", name);

            client.namespaces()
                .withName(name)
                .delete();
        }
    }
}
