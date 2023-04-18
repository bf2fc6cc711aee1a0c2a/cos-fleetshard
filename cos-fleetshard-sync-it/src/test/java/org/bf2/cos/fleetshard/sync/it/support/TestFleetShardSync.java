package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.FleetShardSync;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.Mock;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;

@Mock
@ApplicationScoped
public class TestFleetShardSync extends FleetShardSync {
    public final static String CUSTOM_ADDON_PULL_SECRET_NAME = ConnectorNamespaceProvisioner.DEFAULT_ADDON_PULLSECRET_NAME
        + "-custom";
    public final static String ADDON_SECRET_TYPE = "kubernetes.io/dockerconfigjson";
    public final static String ADDON_SECRET_FIELD = ".dockerconfigjson";
    public final static String ADDON_SECRET_VALUE = "ewogICJhdXRocyI6IHsKICAgICJxdWF5LmlvIjogewogICAgICAiYXV0aCI6ICJjbWh2WVhNclpHVjJaV3h2Y0dWeUxXUmxjR3h2ZVdWeU9rUkJSbFJRVlU1TCIsCiAgICAgICJlbWFpbCI6ICIiCiAgICB9CiAgfQp9Cg==";

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
    public void start() throws Exception {
        LOGGER.info("Creating namespace {}", namespace);

        client.resource(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .endMetadata()
                .build())
            .create();

        Secret addonPullSecret = new Secret();

        ObjectMeta addonPullSecretMetadata = new ObjectMeta();
        addonPullSecretMetadata.setNamespace(namespace);
        addonPullSecretMetadata.setName(ConnectorNamespaceProvisioner.DEFAULT_ADDON_PULLSECRET_NAME);
        addonPullSecret.setMetadata(addonPullSecretMetadata);
        addonPullSecret.setType(ADDON_SECRET_TYPE);
        addonPullSecret.setData(Map.of(ADDON_SECRET_FIELD, ADDON_SECRET_VALUE));

        client.resource(addonPullSecret).inNamespace(namespace).create();

        addonPullSecretMetadata.setName(CUSTOM_ADDON_PULL_SECRET_NAME);
        addonPullSecret.setData(Map.of(ADDON_SECRET_FIELD, ADDON_SECRET_VALUE));

        client.resource(addonPullSecret).inNamespace(namespace).create();

        beforeStart();

        super.start();
    }

    public void beforeStart() {
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        if (namespaceDelete) {
            LOGGER.info("Deleting namespace {}", namespace);

            client.namespaces().withName(namespace).delete();

            for (Namespace ns : client.namespaces().withLabel(LABEL_CLUSTER_ID, clusterId).list().getItems()) {
                LOGGER.info("Deleting namespace {}", ns);
                client.resource(ns).delete();
            }
        }
    }
}
