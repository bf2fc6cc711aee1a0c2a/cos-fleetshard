package org.bf2.cos.fleetshard.sync.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class AddonCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddonCleanup.class);

    public static final String JOB_ID = "cos.connectors.addon.cleanup";
    public static final String METRICS = "connectors.addon.cleanup";

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    MeterRegistry registry;

    private volatile MetricsRecorder recorder;
    private volatile AtomicLong retries = new AtomicLong(0);

    public void start() throws Exception {
        LOGGER.info("Starting cleanup");

        recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + METRICS);

        scheduler.schedule(
            JOB_ID,
            AddonCleanupJob.class,
            config.addon().cleanupRetryInterval());
    }

    public void stop() {
        scheduler.shutdownQuietly(JOB_ID);
    }

    public void run() {
        recorder.record(this::cleanup);
    }

    public void cleanup() {
        if (retries.incrementAndGet() > config.addon().cleanupRetryLimit()) {
            LOGGER.warn("Retry limit ({}) for deletion of namespaces has been reached. The application will signal " +
                "the addon to continue with the uninstall of the operators. This might leave unwanted namespaces " +
                "in the cluster.", retries);
            deleteAddonResource();
        }

        LOGGER.info("Deleting all namespaces that belong to cluster {}. Try #{}", config.cluster().id(), retries);
        getNamespaceFilter().delete();

        if (getNamespaceFilter().list().getItems().isEmpty()) {
            LOGGER.info("All namespaces have been deleted. Deleting {} in namespace {} to signal that addon " +
                "and operators removals should proceed.", config.addon().olmOperatorsKind(), config.namespace());
            deleteAddonResource();
            LOGGER.info("Cluster cleanup should be completed. Addon and Operators removals should proceed normally.");
        } else {
            LOGGER.info("Namespaces still not deleted, will wait for their deletion and try again.");
        }
    }

    private void deleteAddonResource() {
        ResourceDefinitionContext rdc = new ResourceDefinitionContext.Builder()
            .withGroup(config.addon().olmOperatorsGroup())
            .withVersion(config.addon().olmOperatorsApiVersion())
            .withKind(config.addon().olmOperatorsKind())
            .build();

        kubernetesClient.genericKubernetesResources(rdc)
            .inNamespace(config.namespace())
            .delete();
    }

    private FilterWatchListDeletable<Namespace, NamespaceList> getNamespaceFilter() {
        return kubernetesClient.namespaces().withLabel(Resources.LABEL_CLUSTER_ID, config.cluster().id());
    }

}
