package org.bf2.cos.fleetshard.operator.connector;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.fabric8.kubernetes.client.Watch;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.client.MetaClientException;
import org.bf2.cos.fleetshard.support.EventQueue;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.bf2.cos.fleetshard.support.watch.AbstractWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentStatusUpdater.class);

    private final ConnectorStatusQueue queue = new ConnectorStatusQueue();
    private final ConnectorStatusObserver observer = new ConnectorStatusObserver();

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    UnstructuredClient uc;
    @Inject
    ConnectorDeploymentStatusExtractor extractor;

    @ConfigProperty(
        name = "cos.connectors.status.sync.interval")
    String statusSyncInterval;

    void onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        this.observer.start();
    }

    void onShutdown(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent event) {
        this.observer.close();
    }

    public void poison() {
        this.queue.submitPoisonPill();
    }

    public void submit(String managedConnectorName) {
        this.queue.submit(managedConnectorName);
    }

    public void run() {
        final int queueSize = queue.size();
        final Collection<ManagedConnector> connectors = queue.poll();

        LOGGER.debug("Polling ManagedConnector status queue (interval={}, connectors={}, queue_size={})",
            statusSyncInterval,
            connectors.size(),
            queueSize);

        for (ManagedConnector connector : connectors) {
            updateConnectorDeploymentStatus(connector);
        }
    }

    private void updateConnectorDeploymentStatus(ManagedConnector connector) {
        LOGGER.debug("Update connector status (name: {}, phase: {})",
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        try {
            controlPlane.updateConnectorStatus(
                connector,
                extractor.extract(connector));

        } catch (MetaClientException e) {
            LOGGER.warn("Error retrieving status for connector " + connector.getMetadata().getName(), e);
        } catch (FleetManagerClientException e) {
            //TODO: remove 404 after https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-manager/issues/2
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                LOGGER.info("Connector " + connector.getMetadata().getName() + " does not exists anymore, deleting it");
                fleetShard.deleteManagedConnector(connector);
            } else {
                LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
        }
    }

    /**
     * Helper class to watch for ManagedConnector resource update.
     */
    private class ConnectorStatusObserver extends AbstractWatcher<ManagedConnector> {
        @Override
        protected Watch doWatch() {
            return fleetShard.getKubernetesClient()
                .customResources(ManagedConnector.class)
                .inNamespace(fleetShard.getConnectorsNamespace())
                .watch(this);
        }

        @Override
        public void onEventReceived(Action action, ManagedConnector resource) {
            queue.submit(resource.getMetadata().getName());
        }
    }

    /**
     * Helper class to queue events.
     */
    private class ConnectorStatusQueue extends EventQueue<String, ManagedConnector> {
        @Override
        protected Collection<ManagedConnector> collectAll() {
            return fleetShard.lookupManagedConnectors();
        }

        @Override
        protected Collection<ManagedConnector> collectAll(Collection<String> elements) {
            return elements.stream()
                .sorted()
                .distinct()
                .flatMap(e -> fleetShard.lookupManagedConnector(e).stream())
                .collect(Collectors.toList());
        }
    }
}
