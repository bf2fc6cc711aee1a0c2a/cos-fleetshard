package org.bf2.cos.fleetshard.sync.client;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.support.watch.Informers;
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
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

@ApplicationScoped
public class FleetShardClient implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardClient.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;

    private volatile SharedIndexInformer<ManagedConnector> connectorsInformer;
    private volatile SharedIndexInformer<ManagedConnectorOperator> operatorsInformer;
    private volatile SharedIndexInformer<Namespace> namespaceInformers;

    @Override
    public void start() throws Exception {
        operatorsInformer = kubernetesClient.resources(ManagedConnectorOperator.class)
            .inNamespace(config.namespace())
            .inform();
        namespaceInformers = kubernetesClient.namespaces()
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .inform();
        connectorsInformer = kubernetesClient.resources(ManagedConnector.class)
            .inAnyNamespace()
            .withLabel(Resources.LABEL_CLUSTER_ID, getClusterId())
            .inform();
    }

    @Override
    public void stop() throws Exception {
        Resources.closeQuietly(operatorsInformer);
        Resources.closeQuietly(namespaceInformers);
        Resources.closeQuietly(connectorsInformer);
    }

    public String getClusterId() {
        return config.cluster().id();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public long getMaxDeploymentResourceRevision() {
        return this.connectorsInformer.getIndexer().list().stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);
    }

    public long getMaxNamespaceResourceRevision() {
        return this.namespaceInformers.getIndexer().list().stream()
            .mapToLong(c -> {
                String rv = Resources.getAnnotation(c, Resources.ANNOTATION_NAMESPACE_RESOURCE_VERSION);
                if (rv == null) {
                    return 0;
                }

                return Long.parseLong(rv);
            })
            .max()
            .orElse(0);
    }

    // *************************************
    //
    // Namespaces
    //
    // *************************************

    public Optional<Namespace> getNamespace(String namespaceId) {
        return Optional.ofNullable(
            this.kubernetesClient
                .namespaces()
                .withName(generateNamespaceId(namespaceId))
                .get());
    }

    public List<Namespace> getNamespaces() {
        return namespaceInformers != null
            ? namespaceInformers.getIndexer().list()
            : Collections.emptyList();
    }

    public String generateNamespaceId(String namespaceId) {

        String prefix = config.tenancy().namespacePrefix();
        if (!prefix.endsWith("-")) {
            prefix = prefix + "-";
        }

        return namespaceId.startsWith(prefix)
            ? namespaceId
            : prefix + namespaceId;
    }

    public Boolean deleteNamespace(Namespace namespace) {
        return kubernetesClient.namespaces().delete(namespace);
    }

    public void watchNamespaces(Consumer<Namespace> handler) {
        if (namespaceInformers == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        namespaceInformers.addEventHandler(Informers.wrap(handler));
    }

    public void watchNamespaces(ResourceEventHandler<Namespace> handler) {
        if (namespaceInformers == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        namespaceInformers.addEventHandler(handler);
    }

    public Namespace createNamespace(Namespace namespace) {
        return this.kubernetesClient.namespaces()
            .createOrReplace(namespace);
    }

    // *************************************
    //
    // Secrets
    //
    // *************************************

    public Secret createSecret(Secret secret) {
        return this.kubernetesClient.secrets()
            .inNamespace(secret.getMetadata().getNamespace())
            .withName(secret.getMetadata().getName())
            .createOrReplace(secret);
    }

    public Optional<Secret> getSecret(ConnectorDeployment deployment) {
        return getSecret(
            deployment.getSpec().getNamespaceId(),
            deployment.getId());
    }

    public Optional<Secret> getSecret(NamespacedName id) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(id.getNamespace())
                .withName(id.getName())
                .get());
    }

    public Optional<Secret> getSecret(String namespaceId, String deploymentId) {
        return Optional.ofNullable(
            kubernetesClient.secrets()
                .inNamespace(generateNamespaceId(namespaceId))
                .withName(Secrets.generateConnectorSecretId(deploymentId))
                .get());
    }

    // *************************************
    //
    // Connectors
    //
    // *************************************

    public Boolean deleteConnector(ManagedConnector managedConnector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(managedConnector.getMetadata().getNamespace())
            .withName(managedConnector.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND)
            .delete();
    }

    public Optional<ManagedConnector> getConnector(NamespacedName id) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        final String key = Cache.namespaceKeyFunc(id.getNamespace(), id.getName());
        final ManagedConnector val = connectorsInformer.getIndexer().getByKey(key);

        return Optional.ofNullable(val);
    }

    public Optional<ManagedConnector> getConnector(ConnectorDeployment deployment) {
        return getConnector(
            deployment.getSpec().getNamespaceId(),
            deployment.getId());
    }

    public Optional<ManagedConnector> getConnector(String namespaceId, String deploymentId) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        final String key = Cache.namespaceKeyFunc(generateNamespaceId(namespaceId), generateConnectorId(deploymentId));
        final ManagedConnector val = connectorsInformer.getIndexer().getByKey(key);

        return Optional.ofNullable(val);
    }

    public List<ManagedConnector> getAllConnectors() {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        return connectorsInformer.getIndexer().list();
    }

    public List<ManagedConnector> getConnectors(String namespace) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        return connectorsInformer.getIndexer().byIndex(Cache.NAMESPACE_INDEX, namespace);
    }

    public List<ManagedConnector> getConnectors(Namespace namespace) {
        return getConnectors(namespace.getMetadata().getName());
    }

    public void watchConnectors(Consumer<ManagedConnector> handler) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        connectorsInformer.addEventHandler(Informers.wrap(handler));
    }

    public void watchConnectors(ResourceEventHandler<ManagedConnector> handler) {
        if (connectorsInformer == null) {
            throw new IllegalStateException("Informer must be started before adding handlers");
        }

        connectorsInformer.addEventHandler(handler);
    }

    public ManagedConnector createConnector(ManagedConnector connector) {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .createOrReplace(connector);
    }

    public String generateConnectorId(String namespaceId) {
        return Connectors.generateConnectorId(namespaceId);
    }

    // *************************************
    //
    // Operators
    //
    // *************************************

    public List<ManagedConnectorOperator> getOperators() {
        return operatorsInformer != null
            ? operatorsInformer.getIndexer().list()
            : Collections.emptyList();
    }

    // *************************************
    //
    // Cluster
    //
    // *************************************

    public Optional<ManagedConnectorCluster> getConnectorCluster() {
        return Optional.ofNullable(
            kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(this.config.namespace())
                .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + getClusterId())
                .get());
    }

    public ManagedConnectorCluster getOrCreateManagedConnectorCluster() {
        return getConnectorCluster().orElseGet(() -> {
            var cluster = new ManagedConnectorClusterBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + getClusterId())
                    .addToLabels(Resources.LABEL_CLUSTER_ID, getClusterId())
                    .build())
                .withSpec(new ManagedConnectorClusterSpecBuilder()
                    .withClusterId(getClusterId())
                    .build())
                .build();

            return kubernetesClient.resources(ManagedConnectorCluster.class)
                .inNamespace(this.config.namespace())
                .withName(cluster.getMetadata().getName())
                .createOrReplace(cluster);
        });
    }

    // *************************************
    //
    // Event
    //
    // *************************************

    public void broadcast(String type, String reason, String message, HasMetadata involved) {
        ObjectReference ref = new ObjectReference();
        ref.setApiVersion(involved.getApiVersion());
        ref.setKind(involved.getKind());
        ref.setName(involved.getMetadata().getName());
        ref.setNamespace(involved.getMetadata().getNamespace());
        ref.setUid(involved.getMetadata().getUid());

        Event event = new Event();
        event.setType(type);
        event.setReason(reason);
        event.setMessage(message);
        event.setInvolvedObject(ref);
        event.setLastTimestamp(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        KubernetesResourceUtil.getOrCreateMetadata(event).setGenerateName("cos-fleetshard-sync");
        KubernetesResourceUtil.getOrCreateMetadata(event).setNamespace(involved.getMetadata().getNamespace());

        broadcast(event);
    }

    public void broadcast(Event event) {
        try {
            kubernetesClient.v1()
                .events()
                .inNamespace(event.getMetadata().getNamespace())
                .create(event);
        } catch (Exception e) {
            LOGGER.warn("Error while broadcasting events", e);
        }
    }

    public Optional<Observability> getObservability() {
        return Optional.ofNullable(
            kubernetesClient.resources(Observability.class)
                .inNamespace(this.config.namespace())
                .withName("rhoc-observability-stack")
                .get());
    }

    public void setupObservability() {
        final var observability = getObservability();
        if (observability.isPresent()) {
            ObjectMeta metadata = observability.get().getMetadata();
            LOGGER.warn("Observability resource was already created: {}/{}", metadata.getNamespace(), metadata.getName());
            return;
        }

        if (!config.observability().enabled()) {
            LOGGER.warn("Observability is not enabled.");
            return;
        }

        var observabilityCRD = kubernetesClient.resources(CustomResourceDefinition.class)
            .withName("observabilities.observability.redhat.com")
            .get();
        if (observabilityCRD == null) {
            LOGGER
                .error("Observability will not be enabled because the Observability CustomResourceDefinition is not present.");
            return;
        }

        createObservabilityResource();
    }

    private void createObservabilityResource() {
        LOGGER.info("Creating Observability resource");
        final var observability = new Observability();

        final var meta = new ObjectMetaBuilder()
            .withName("rhoc-observability-stack")
            .withNamespace(this.config.namespace())
            .withFinalizers("observability-cleanup")
            .build();
        observability.setMetadata(meta);

        final var spec = new ObservabilitySpec();
        spec.setClusterId(getClusterId());

        final var configurationSelector = new ConfigurationSelector();
        configurationSelector.setMatchLabels(Map.of("configures", "observability-operator"));
        spec.setConfigurationSelector(configurationSelector);

        spec.setResyncPeriod("60m");
        spec.setRetention("30d");

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
        resources.setRequests(Map.of("storage", new IntOrString("100Gi")));
        volumeClaimTemplateSpec.setResources(resources);
        volumeClaimTemplate.setSpec(volumeClaimTemplateSpec);
        prometheus.setVolumeClaimTemplate(volumeClaimTemplate);
        storage.setPrometheus(prometheus);

        spec.setStorage(storage);

        observability.setSpec(spec);

        kubernetesClient.resources(Observability.class)
            .inNamespace(this.config.namespace())
            .withName(observability.getMetadata().getName())
            .createOrReplace(observability);
    }

}
