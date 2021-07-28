package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.fabric8.kubernetes.client.Watch;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.support.EventQueue;
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterStatusUpdater.class);

    private final ConnectorClusterStatusQueue queue = new ConnectorClusterStatusQueue();
    private final ConnectorClusterStatusObserver observer = new ConnectorClusterStatusObserver();

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;

    void onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        this.observer.start();
    }

    void onShutdown(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent event) {
        this.observer.close();
    }

    public void poison() {
        this.queue.submitPoisonPill();
    }

    public void run() {
        final int queueSize = queue.size();
        final Collection<ManagedConnectorCluster> clusters = queue.poll();

        LOGGER.debug("Polling ManagedConnectorCluster status queue (clusters={}, queue_size={})",
            clusters.size(),
            queueSize);

        for (ManagedConnectorCluster cluster : clusters) {
            updateConnectorDeploymentStatus(cluster);
        }
    }

    private void updateConnectorDeploymentStatus(ManagedConnectorCluster cluster) {
        if (!Objects.equals(cluster.getSpec().getId(), fleetShard.getClusterId())) {
            return;
        }

        LOGGER.debug("Update cluster status (name: {}, phase: {})",
            cluster.getMetadata().getName(),
            cluster.getStatus().getPhase());

        try {
            controlPlane.updateClusterStatus(
                cluster,
                fleetShard.lookupManagedConnectorOperators());

        } catch (Exception e) {
            LOGGER.warn("Error updating status of cluster " + cluster.getMetadata().getName(), e);
        }
    }

    /**
     * Helper class to watch for ManagedConnector resource update.
     */
    private class ConnectorClusterStatusObserver extends AbstractWatcher<ManagedConnectorCluster> {
        @Override
        protected Watch doWatch() {
            return fleetShard.getKubernetesClient()
                .customResources(ManagedConnectorCluster.class)
                .inNamespace(fleetShard.getConnectorsNamespace())
                .watch(this);
        }

        @Override
        public void onEventReceived(Action action, ManagedConnectorCluster resource) {
            LOGGER.debug("onEventReceived >>>> {}", resource);
            queue.submit(resource.getMetadata().getName());
        }
    }

    /**
     * Helper class to queue events.
     */
    private class ConnectorClusterStatusQueue extends EventQueue<String, ManagedConnectorCluster> {
        @Override
        protected Collection<ManagedConnectorCluster> collectAll() {
            return fleetShard.lookupManagedConnectorClusters(fleetShard.getClusterNamespace());
        }

        @Override
        protected Collection<ManagedConnectorCluster> collectAll(Collection<String> elements) {
            return fleetShard.lookupManagedConnectorClusters(fleetShard.getClusterNamespace());
        }
    }
}
