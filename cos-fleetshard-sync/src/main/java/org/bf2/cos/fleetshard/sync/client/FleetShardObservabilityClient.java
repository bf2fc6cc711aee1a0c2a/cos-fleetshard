package org.bf2.cos.fleetshard.sync.client;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.observability.v1.Observability;
import com.redhat.observability.v1.ObservabilitySpec;
import com.redhat.observability.v1.observabilityspec.ConfigurationSelector;
import com.redhat.observability.v1.observabilityspec.DescopedMode;
import com.redhat.observability.v1.observabilityspec.SelfContained;
import com.redhat.observability.v1.observabilityspec.Storage;
import com.redhat.observability.v1.observabilityspec.selfcontained.*;
import com.redhat.observability.v1.observabilityspec.storage.Prometheus;
import com.redhat.observability.v1.observabilityspec.storage.prometheus.VolumeClaimTemplate;
import com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.Spec;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class FleetShardObservabilityClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardObservabilityClient.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    public void setupObservability() {
        if (!config.observability().enabled()) {
            LOGGER.warn("Observability is not enabled.");
            return;
        }

        var observabilityCRD = kubernetesClient.resources(CustomResourceDefinition.class)
            .withName("observabilities.observability.redhat.com")
            .get();
        if (observabilityCRD == null) {
            LOGGER.error("Observability CustomResourceDefinition is not present. Observability will not be enabled.");
            return;
        }

        createObservatoriumSecret();
        createObservabilityResource();
    }

    public void cleanUp() {
        LOGGER.info("Cleaning up Observability resources.");
        getObservabilityFilter().delete();
        getObservabilityNamespaceFilter().delete();
    }

    public boolean isCleanedUp() {
        return getObservabilityFilter().get() == null && getObservabilityNamespaceFilter().get() == null;
    }

    private void createObservatoriumSecret() {
        LOGGER.info("Configuring Observatorium SSO secret");
        final var fromSecret = kubernetesClient.secrets()
            .inNamespace(config.namespace())
            .withName(config.observability().observatoriumSecretName() + "-" + config.observability().environment())
            .get();

        if (fromSecret != null) {
            final var obsSecret = new Secret();

            final var meta = new ObjectMetaBuilder()
                .withName(config.observability().observatoriumSecretName())
                .withNamespace(config.namespace())
                .build();
            obsSecret.setMetadata(meta);

            obsSecret.setData(fromSecret.getData());
            kubernetesClient.secrets()
                .inNamespace(obsSecret.getMetadata().getNamespace())
                .withName(obsSecret.getMetadata().getNamespace())
                .createOrReplace(obsSecret);
        } else {
            LOGGER.warn("Observatorium secret for environment {} not found.", config.observability().environment());
        }
    }

    private void createObservabilityResource() {
        LOGGER.info("Creating Observability resource");
        final var observability = new Observability();

        final var meta = new ObjectMetaBuilder()
            .withName(config.observability().resourceName())
            .withNamespace(config.namespace())
            .build();
        observability.setMetadata(meta);

        final var spec = new ObservabilitySpec();

        final var descopedMode = new DescopedMode();
        descopedMode.setEnabled(true);
        descopedMode.setPrometheusOperatorNamespace(config.observability().namespace());
        spec.setDescopedMode(descopedMode);

        spec.setClusterId(config.cluster().id());

        final var configurationSelector = new ConfigurationSelector();
        configurationSelector.setMatchLabels(Map.of("configures", config.observability().configuresMatchLabel()));
        spec.setConfigurationSelector(configurationSelector);

        spec.setResyncPeriod(config.observability().resyncPeriod());
        spec.setRetention(config.observability().retention());

        final var selfContained = new SelfContained();
        selfContained.setDisablePagerDuty(false);
        selfContained.setDisableSmtp(true);

        selfContained.setPrometheusVersion("v2.35.0");

        Map<String, String> rhocAppLabel = Map.of("app", "rhoc");

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

        getObservabilityFilter().createOrReplace(observability);
        LOGGER.info("Observability resource created");
    }

    private Resource<Observability> getObservabilityFilter() {
        return kubernetesClient.resources(Observability.class)
            .inNamespace(config.namespace())
            .withName(config.observability().resourceName());
    }

    private Resource<Namespace> getObservabilityNamespaceFilter() {
        return kubernetesClient.namespaces()
            .withName(config.observability().namespace());
    }

}
