package org.bf2.cos.fleetshard.operator.it.support;

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
public class TestFleetShardOperator extends FleetShardOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFleetShardOperator.class);

    @Inject
    KubernetesClient client;
    @Inject
    CosFeatureContext cosFeatureContext;

    @ConfigProperty(name = "test.namespace")
    String namespace;

    @ConfigProperty(name = "test.namespace.delete", defaultValue = "true")
    boolean namespaceDelete;

    @Override
    public void start() {
        LOGGER.info("Creating test namespaces");

        if (!existsNamespace(namespace)) {
            LOGGER.info("Creating namespace {}", namespace);
            client.namespaces().create(
                new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build());
        }

        if (!existsNamespace(cosFeatureContext.getConnectorsNamespace())) {
            LOGGER.info("Creating namespace {}", namespace);
            client.namespaces().create(
                new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(cosFeatureContext.getConnectorsNamespace())
                    .endMetadata()
                    .build());
        }

        if (!existsNamespace(cosFeatureContext.getOperatorsNamespace())) {
            LOGGER.info("Creating namespace {}", namespace);
            client.namespaces().create(
                new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(cosFeatureContext.getOperatorsNamespace())
                    .endMetadata()
                    .build());
        }

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        LOGGER.info("Deleting test namespaces");
        if (namespaceDelete) {
            LOGGER.info("Deleting namespace {}", namespace);
            client.namespaces().withName(namespace).delete();
            LOGGER.info("Deleting namespace {}", cosFeatureContext.getConnectorsNamespace());
            client.namespaces().withName(cosFeatureContext.getConnectorsNamespace()).delete();
            LOGGER.info("Deleting namespace {}", cosFeatureContext.getOperatorsNamespace());
            client.namespaces().withName(cosFeatureContext.getOperatorsNamespace()).delete();
        }
    }

    private boolean existsNamespace(String name) {
        return client.namespaces().withName(name).get() != null;
    }
}
