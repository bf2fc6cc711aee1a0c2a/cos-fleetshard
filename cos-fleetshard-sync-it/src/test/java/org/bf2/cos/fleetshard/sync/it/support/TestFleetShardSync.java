package org.bf2.cos.fleetshard.sync.it.support;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.FleetShardSync;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;

@Mock
@ApplicationScoped
public class TestFleetShardSync extends FleetShardSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFleetShardSync.class);

    @Inject
    KubernetesClient client;

    @ConfigProperty(name = "test.namespace")
    String namespace;

    @ConfigProperty(name = "test.namespace.delete", defaultValue = "true")
    boolean namespaceDelete;

    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;

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

            for (Namespace ns : client.namespaces().withLabel(LABEL_CLUSTER_ID, clusterId).list().getItems()) {
                LOGGER.info("Deleting namespace {}", ns);
                client.namespaces().delete(ns);
            }
        }
    }
}
