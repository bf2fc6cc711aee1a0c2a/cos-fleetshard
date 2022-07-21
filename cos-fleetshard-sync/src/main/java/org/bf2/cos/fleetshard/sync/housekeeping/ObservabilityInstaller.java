package org.bf2.cos.fleetshard.sync.housekeeping;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.observability.v1.Observability;
import com.redhat.observability.v1.ObservabilitySpec;
import com.redhat.observability.v1.observabilityspec.ConfigurationSelector;
import com.redhat.observability.v1.observabilityspec.SelfContained;
import com.redhat.observability.v1.observabilityspec.Storage;
import com.redhat.observability.v1.observabilityspec.selfcontained.*;
import com.redhat.observability.v1.observabilityspec.storage.Prometheus;
import com.redhat.observability.v1.observabilityspec.storage.prometheus.VolumeClaimTemplate;
import com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.Spec;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.*;

@ApplicationScoped
public class ObservabilityInstaller implements Housekeeper.Task, Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityInstaller.class);

    private static final String TASK_ID = "observability.installer";

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    private final AtomicBoolean running;
    private final AtomicBoolean taskRunning;

    public ObservabilityInstaller(KubernetesClient kubernetesClient, FleetShardSyncConfig config) {
        this.kubernetesClient = kubernetesClient;
        this.config = config;
        this.running = new AtomicBoolean();
        this.taskRunning = new AtomicBoolean();
    }

    @Override
    public void start() throws Exception {
        if (!config.observability().enabled()) {
            LOGGER.warn("Observability is not enabled. Will not create observability resources.");
        }
        if (config.observability().subscription().removePrevious()) {
            LOGGER.warn("Removal of previous Subscription is enabled on namespace {}.",
                config.observability().removalNamespace());
        }
        this.running.set(true);
    }

    @Override
    public void stop() throws Exception {
        this.running.set(false);
    }

    @Override
    public String getId() {
        return TASK_ID;
    }

    @Override
    public void run() {
        if (running.get() && taskRunning.compareAndSet(false, true)) {
            try {
                setupObservability();
            } finally {
                taskRunning.set(false);
            }
        }
    }

    private void setupObservability() {
        if (config.observability().subscription().removePrevious()) {
            final String installedCSV = removePreviousSubscriptionResource();
            if (!validateSubscriptionRemoved(installedCSV)) {
                LOGGER.info("Waiting on Subscription removal before proceeding with observability setup.");
                return;
            }
        }

        if (!config.observability().enabled()) {
            LOGGER.debug("Observability is not enabled. Will not create observability resources.");
            return;
        }

        final var observabilityCRD = kubernetesClient.resources(CustomResourceDefinition.class)
            .withName("observabilities.observability.redhat.com")
            .get();
        if (observabilityCRD == null) {
            LOGGER.error("Observability CustomResourceDefinition is not present. Will not create observability resources.");
            return;
        }

        if (config.observability().namespace().isBlank()) {
            LOGGER.error("Observability namespace is not set! Will not create observability resources.");
            return;
        }

        if (config.observability().subscription().enabled()) {
            createSubscriptionResource();
        }
        copyResourcesToObservabilityNamespace();
        createObservabilityResource();
    }

    /**
     * @return the installedCSV name.
     */
    private String removePreviousSubscriptionResource() {
        LOGGER.debug("Removing previously installed Subscription and CSV.");
        final Subscription subscription = getSubscriptionForRemoval();

        if (subscription == null) {
            LOGGER.debug("There was no Subscription resource to remove.");
            return null;
        }

        SubscriptionStatus status = subscription.getStatus();
        if (status == null) {
            LOGGER.warn("Subscription has no status, can't proceed with removal.");
            return null;
        }

        kubernetesClient.resources(Subscription.class)
            .inNamespace(config.observability().removalNamespace())
            .withName(config.observability().subscription().name())
            .delete();

        final String installedCSV = status.getInstalledCSV();
        if (installedCSV != null && !installedCSV.isBlank()) {
            kubernetesClient.resources(ClusterServiceVersion.class)
                .inNamespace(config.observability().removalNamespace())
                .withName(installedCSV)
                .delete();
        } else {
            LOGGER.debug("There was no installedCSV to remove.");
        }

        return installedCSV;
    }

    private boolean validateSubscriptionRemoved(String installedCSV) {
        final Subscription subscription = getSubscriptionForRemoval();
        if (subscription == null) {
            return installedCSV == null || installedCSV.isBlank() || getCsvForRemoval(installedCSV) == null;
        }
        return false;
    }

    private ClusterServiceVersion getCsvForRemoval(String installedCSV) {
        return kubernetesClient.resources(ClusterServiceVersion.class)
            .inNamespace(config.observability().removalNamespace())
            .withName(installedCSV)
            .get();
    }

    private Subscription getSubscriptionForRemoval() {
        return kubernetesClient.resources(Subscription.class)
            .inNamespace(config.observability().removalNamespace())
            .withName(config.observability().subscription().name())
            .get();
    }

    private void createSubscriptionResource() {
        LOGGER.debug("Creating Subscription resource");
        final FleetShardSyncConfig.Observability.Subscription subscriptionConfig = config.observability().subscription();

        final Subscription subscription = new SubscriptionBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(subscriptionConfig.name())
                    .withNamespace(config.observability().namespace())
                    .build())
            .withSpec(
                new SubscriptionSpecBuilder()
                    .withName(subscriptionConfig.name())
                    .withChannel(subscriptionConfig.channel())
                    .withInstallPlanApproval(subscriptionConfig.installPlanApproval())
                    .withSource(subscriptionConfig.source())
                    .withSourceNamespace(subscriptionConfig.sourceNamespace())
                    .withStartingCSV(subscriptionConfig.startingCsv())
                    .build())
            .build();

        kubernetesClient.resources(Subscription.class)
            .inNamespace(subscription.getMetadata().getNamespace())
            .withName(subscription.getMetadata().getName())
            .createOrReplace(subscription);
        LOGGER.debug("Subscription resource created");
    }

    private void copyResourcesToObservabilityNamespace() {
        LOGGER.debug("Copying resources to observability namespace");

        config.observability().configMapsToCopy().ifPresent(resources -> resources.forEach(this::copyConfigMap));
        config.observability().secretsToCopy().ifPresent(resources -> resources.forEach(this::copySecret));

        LOGGER.debug("Observability resources copied to the target namespace: {}", config.observability().namespace());
    }

    private void copyConfigMap(String resourceName) {
        final ConfigMap resource = kubernetesClient.resources(ConfigMap.class)
            .inNamespace(config.namespace())
            .withName(resourceName)
            .get();

        if (resource == null) {
            final String msg = String.format("Observability ConfigMap not found to be copied: %s/%s", config.namespace(),
                resourceName);
            throw new IllegalArgumentException(msg);
        }

        resource.getMetadata().setNamespace(config.observability().namespace());
        kubernetesClient.resources(ConfigMap.class)
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resourceName)
            .createOrReplace(resource);
    }

    private void copySecret(String resourceName) {
        final Secret resource = kubernetesClient.resources(Secret.class)
            .inNamespace(config.namespace())
            .withName(resourceName)
            .get();

        if (resource == null) {
            String msg = String.format("Observability Secret not found to be copied: %s/%s", config.namespace(), resourceName);
            throw new IllegalArgumentException(msg);
        }

        resource.getMetadata().setNamespace(config.observability().namespace());
        kubernetesClient.resources(Secret.class)
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resourceName)
            .createOrReplace(resource);
    }

    private void createObservabilityResource() {
        LOGGER.debug("Creating Observability resource");
        final var observability = new Observability();

        final var meta = new ObjectMetaBuilder()
            .withName(config.observability().resourceName())
            .withNamespace(config.observability().namespace())
            .withFinalizers(config.observability().finalizer())
            .build();
        observability.setMetadata(meta);

        final var spec = new ObservabilitySpec();
        spec.setClusterId(config.cluster().id());

        final var configurationSelector = new ConfigurationSelector();
        configurationSelector.setMatchLabels(Map.of("configures", config.observability().configuresMatchLabel()));
        spec.setConfigurationSelector(configurationSelector);

        spec.setResyncPeriod(config.observability().resyncPeriod());
        spec.setRetention(config.observability().retention());

        final var selfContained = new SelfContained();
        selfContained.setDisablePagerDuty(false);

        Map<String, String> rhocAppLabel = Map.of("app", "rhoc");

        final var grafanaDashboardLS = new GrafanaDashboardLabelSelector();
        grafanaDashboardLS.setMatchLabels(rhocAppLabel);
        selfContained.setGrafanaDashboardLabelSelector(grafanaDashboardLS);

        final var podMonitorLS = new PodMonitorLabelSelector();
        podMonitorLS.setMatchLabels(rhocAppLabel);
        selfContained.setPodMonitorLabelSelector(podMonitorLS);

        final var ruleLS = new RuleLabelSelector();
        ruleLS.setMatchLabels(rhocAppLabel);
        selfContained.setRuleLabelSelector(ruleLS);

        final var serviceMonitorLS = new ServiceMonitorLabelSelector();
        serviceMonitorLS.setMatchLabels(rhocAppLabel);
        selfContained.setServiceMonitorLabelSelector(serviceMonitorLS);

        final var probeSelector = new ProbeSelector();
        probeSelector.setMatchLabels(rhocAppLabel);
        selfContained.setProbeSelector(probeSelector);

        spec.setSelfContained(selfContained);

        final var storage = new Storage();
        final var prometheus = new Prometheus();
        final var volumeClaimTemplate = new VolumeClaimTemplate();
        final var volumeClaimTemplateSpec = new Spec();
        final var resources = new com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.spec.Resources();
        resources.setRequests(Map.of("storage", new IntOrString(config.observability().storageRequest())));
        volumeClaimTemplateSpec.setResources(resources);
        volumeClaimTemplate.setSpec(volumeClaimTemplateSpec);
        prometheus.setVolumeClaimTemplate(volumeClaimTemplate);
        storage.setPrometheus(prometheus);

        spec.setStorage(storage);

        observability.setSpec(spec);

        kubernetesClient.resources(Observability.class)
            .inNamespace(observability.getMetadata().getNamespace())
            .withName(observability.getMetadata().getName())
            .createOrReplace(observability);
        LOGGER.debug("Observability resource created");
    }

}
