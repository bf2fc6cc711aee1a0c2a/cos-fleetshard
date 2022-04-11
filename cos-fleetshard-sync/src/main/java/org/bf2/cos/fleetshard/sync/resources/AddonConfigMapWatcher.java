package org.bf2.cos.fleetshard.sync.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.bf2.cos.fleetshard.sync.FleetShardSync;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;

@ApplicationScoped
public class AddonConfigMapWatcher extends AbstractWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddonConfigMapWatcher.class);
    private static final String LABEL_PREFIX = "api.openshift.com/";
    private static final String DELETE_LABEL_SUFFIX = "-delete";

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    FleetShardSync fleetShardSync;

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
    protected void onEventReceived(Action action, Object resource) {
        LOGGER.info("Event received for Addon ConfigMap with action {}", action.name());
        switch (action) {
            case ADDED:
            case MODIFIED:
                cleanupCluster((ConfigMap) resource);
                break;
            case DELETED:
            case ERROR:
            case BOOKMARK:
                LOGGER.debug("Event {} not covered by this watcher, nothing will be done.", action.name());
                break;
        }
    }

    private void cleanupCluster(ConfigMap configMap) {
        String labelName = LABEL_PREFIX + config.addon().label() + DELETE_LABEL_SUFFIX;
        String deleteLabel = configMap.getMetadata().getLabels().get(labelName);
        if ("true".equals(deleteLabel)) {
            LOGGER.info("ConfigMap for deletion of the Addon was found, starting cleanup of cluster: {} - {}",
                config.cluster().id(), configMap);
            fleetShardSync.startCleanup();
        } else {
            LOGGER.info("Delete label ({}) not found in ConfigMap. Cleanup won't be executed.", labelName);
        }
    }

}
