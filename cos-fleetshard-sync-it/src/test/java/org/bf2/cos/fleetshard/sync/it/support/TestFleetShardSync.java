package org.bf2.cos.fleetshard.sync.it.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.FleetShardSync;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.*;
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

    @ConfigProperty(name = "cos.observability.secrets-to-copy")
    Optional<List<String>> secretsToCopy;

    @ConfigProperty(name = "cos.observability.config-maps-to-copy")
    Optional<List<String>> configMapsToCopy;

    @ConfigProperty(name = "cos.observability.subscription.remove-previous")
    Optional<Boolean> removePreviousSubscription;

    @Inject
    FleetShardSyncConfig config;

    @Override
    public void start() throws Exception {
        LOGGER.info("Creating namespace {}", namespace);

        client.namespaces().create(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .endMetadata()
                .build());

        Secret addonPullSecret = new Secret();

        ObjectMeta addonPullSecretMetadata = new ObjectMeta();
        addonPullSecretMetadata.setNamespace(namespace);
        addonPullSecretMetadata.setName(ConnectorNamespaceProvisioner.DEFAULT_ADDON_PULLSECRET_NAME);
        addonPullSecret.setMetadata(addonPullSecretMetadata);
        addonPullSecret.setType(ADDON_SECRET_TYPE);
        addonPullSecret.setData(Map.of(ADDON_SECRET_FIELD, ADDON_SECRET_VALUE));

        client.secrets().inNamespace(namespace).create(addonPullSecret);

        addonPullSecretMetadata.setName(CUSTOM_ADDON_PULL_SECRET_NAME);
        addonPullSecret.setData(Map.of(ADDON_SECRET_FIELD, ADDON_SECRET_VALUE));

        client.secrets().inNamespace(namespace).create(addonPullSecret);

        secretsToCopy.ifPresent(
            secretsNames -> secretsNames.forEach(
                secretName -> client.secrets().inNamespace(namespace).create(
                    new SecretBuilder().withMetadata(new ObjectMetaBuilder().withName(secretName).build()).build())));

        configMapsToCopy.ifPresent(
            configMapNames -> configMapNames.forEach(
                configMapName -> client.configMaps().inNamespace(namespace).create(
                    new ConfigMapBuilder().withMetadata(new ObjectMetaBuilder().withName(configMapName).build()).build())));

        removePreviousSubscription.ifPresent(this::createSubscriptionResource);

        super.start();
    }

    private void createSubscriptionResource(boolean removePreviousSubscription) {
        // create subscription so the tests can remove it
        if (removePreviousSubscription) {
            final Subscription subscription = new SubscriptionBuilder()
                .withMetadata(
                    new ObjectMetaBuilder()
                        .withName(config.observability().subscription().name())
                        .withNamespace(config.observability().removalNamespace())
                        .build())
                .withSpec(
                    new SubscriptionSpecBuilder()
                        .withName(config.observability().subscription().name())
                        .withChannel("alpha")
                        .withInstallPlanApproval("Manual")
                        .withSource("source")
                        .withSourceNamespace("source")
                        .withStartingCSV("csv")
                        .build())
                .withStatus(new SubscriptionStatusBuilder()
                    .withInstalledCSV("installedCSV")
                    .withLastUpdated("2022-07-11T16:52:35Z")
                    .build())
                .build();

            client.resources(Subscription.class)
                .inNamespace(subscription.getMetadata().getNamespace())
                .withName(subscription.getMetadata().getName())
                .create(subscription);

            client.resources(Subscription.class)
                .inNamespace(subscription.getMetadata().getNamespace())
                .withName(subscription.getMetadata().getName())
                .replaceStatus(subscription);

            SyncTestSupport.until(
                () -> Optional.ofNullable(client.resources(Subscription.class)
                    .inNamespace(subscription.getMetadata().getNamespace())
                    .withName(subscription.getMetadata().getName())
                    .get()),
                Objects::nonNull);
        }

    }

    @Override
    public void stop() throws Exception {
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
