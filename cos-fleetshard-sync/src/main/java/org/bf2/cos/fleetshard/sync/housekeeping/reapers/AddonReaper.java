package org.bf2.cos.fleetshard.sync.housekeeping.reapers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

import org.apache.http.annotation.Obsolete;
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.bf2.cos.fleetshard.sync.FleetShardSync;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.housekeeping.Housekeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

@ApplicationScoped
public class AddonReaper implements Housekeeper.Task, Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddonReaper.class);
    public static final String ID = "addon.cleanup";
    public static final String CLEANUP_EVENT_REASON = "CleaningUpResources";
    public static final String FAILED_CLEANUP_EVENT_REASON = "FailedToCleanUpResources";

    private final KubernetesClient kubernetesClient;
    private final FleetShardSyncConfig config;
    private final FleetShardSync fleetShardSync;
    private final AddonConfigMapWatcher watcher;
    private final AtomicLong retries;
    private final AtomicBoolean running;
    private final AtomicBoolean taskRunning;
    private final EventClient eventClient;

    public AddonReaper(KubernetesClient kubernetesClient, FleetShardSyncConfig config, FleetShardSync fleetShardSync,
        EventClient eventClient) {
        this.kubernetesClient = kubernetesClient;
        this.config = config;
        this.fleetShardSync = fleetShardSync;
        this.eventClient = eventClient;
        this.watcher = new AddonConfigMapWatcher();
        this.retries = new AtomicLong(0);
        this.running = new AtomicBoolean();
        this.taskRunning = new AtomicBoolean();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Obsolete
    public void start() throws Exception {
        this.watcher.start();
    }

    @Override
    public void stop() throws Exception {
        if (this.watcher != null) {
            Resources.closeQuietly(this.watcher);
        }

        this.running.set(false);
        this.taskRunning.set(false);
        this.retries.set(0L);
    }

    @Override
    public void run() {
        run(false);
    }

    public void run(boolean force) {
        if (force || running.get() && taskRunning.compareAndSet(false, true)) {
            try {
                doRun();
            } finally {
                taskRunning.set(false);
            }
        }
    }

    private void doRun() {
        if (retries.incrementAndGet() > config.addon().cleanupRetryLimit()) {
            eventClient.broadcastWarning(FAILED_CLEANUP_EVENT_REASON,
                "Retry limit (%s) for deletion of namespaces has been reached. The application will signal " +
                    "the addon to continue with the uninstall of the operators. This might leave unwanted namespaces " +
                    "in the cluster.",
                retries);

            deleteAddonResource();
        }

        eventClient.broadcastNormal(CLEANUP_EVENT_REASON,
            "Deleting all namespaces that belong to cluster {}. Try #{}",
            config.cluster().id(), retries);
        getNamespaceFilter().delete();

        eventClient.broadcastNormal(CLEANUP_EVENT_REASON, "Asking for Observability clean up");

        if (getNamespaceFilter().list().getItems().isEmpty()) {
            eventClient.broadcastNormal(CLEANUP_EVENT_REASON,
                "All namespaces have been deleted. Deleting %s in namespace %s to signal that addon " +
                    "and operators removals should proceed.",
                config.addon().olmOperatorsKind(),
                config.namespace());

            deleteAddonResource();
            eventClient.broadcastNormal(CLEANUP_EVENT_REASON,
                "Cluster cleanup should be completed. Addon and Operators removals should proceed normally.");
        } else {
            eventClient.broadcastNormal(CLEANUP_EVENT_REASON,
                "Namespaces still not deleted, will wait for their deletion and try again.");
        }
    }

    private void deleteAddonResource() {
        ResourceDefinitionContext rdc = new ResourceDefinitionContext.Builder()
            .withGroup(config.addon().olmOperatorsGroup())
            .withVersion(config.addon().olmOperatorsApiVersion())
            .withKind(config.addon().olmOperatorsKind())
            .withNamespaced(true)
            .build();

        kubernetesClient.genericKubernetesResources(rdc)
            .inNamespace(config.namespace())
            .delete();
    }

    private FilterWatchListDeletable<Namespace, NamespaceList, Resource<Namespace>> getNamespaceFilter() {
        return kubernetesClient.namespaces().withLabel(Resources.LABEL_CLUSTER_ID, config.cluster().id());
    }

    private class AddonConfigMapWatcher extends AbstractWatcher<ConfigMap> {
        private static final String LABEL_PREFIX = "api.openshift.com/";
        private static final String DELETE_LABEL_SUFFIX = "-delete";

        @Override
        protected Watch doWatch() {
            LOGGER.info("Creating Watcher for Addon ConfigMaps on namespace {} with Addon ID {}",
                config.namespace(), config.addon().id());

            return kubernetesClient
                .configMaps()
                .inNamespace(config.namespace())
                .withName(config.addon().id())
                .watch(this);
        }

        @Override
        protected void onEventReceived(Action action, ConfigMap resource) {
            LOGGER.info("Event received for Addon ConfigMap with action {}", action.name());
            switch (action) {
                case ADDED:
                case MODIFIED:
                    cleanupCluster(resource);
                    break;
                case DELETED:
                case ERROR:
                case BOOKMARK:
                    LOGGER.debug("Event {} not covered by this watcher, nothing will be done.", action.name());
                    break;
            }
        }

        private void cleanupCluster(ConfigMap configMap) {
            final String labelName = LABEL_PREFIX + config.addon().label() + DELETE_LABEL_SUFFIX;
            final String deleteLabel = Resources.getLabel(configMap, labelName);

            if ("true".equals(deleteLabel)) {
                eventClient.broadcastNormal(CLEANUP_EVENT_REASON,
                    "ConfigMap for deletion of the Addon was found, starting cleanup of cluster: %s - %s",
                    configMap,
                    config.cluster().id(),
                    configMap);
                try {
                    fleetShardSync.stopResourcesSync();
                } catch (Exception e) {
                    LOGGER.warn("Failure stopping sync tasks", e);
                } finally {
                    running.set(true);
                    run();
                }
            } else {
                LOGGER.info("Delete label ({}) not found in ConfigMap. Cleanup won't be executed.", labelName);
            }
        }

    }
}
