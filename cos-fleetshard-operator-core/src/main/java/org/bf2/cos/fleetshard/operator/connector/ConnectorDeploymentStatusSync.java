package org.bf2.cos.fleetshard.operator.connector;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.Watch;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.api.model.cp.MetaV1Condition;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentStatusRequest;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.client.MetaClient;
import org.bf2.cos.fleetshard.operator.client.MetaClientException;
import org.bf2.cos.fleetshard.operator.support.AbstractWatcher;
import org.bf2.cos.fleetshard.operator.support.OperatorSupport;
import org.bf2.cos.fleetshard.support.UnstructuredClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the status synchronization protocol for the connectors.
 */
@Startup
@ApplicationScoped
public class ConnectorDeploymentStatusSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentStatusSync.class);
    private final ConnectorStatusObserver observer;
    private final ConnectorStatusQueue queue;

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    FleetShardOperator operator;
    @Inject
    UnstructuredClient uc;
    @Inject
    MetaClient meta;

    @ConfigProperty(
        name = "cos.connectors.status.sync.batch.enabled",
        defaultValue = "true")
    boolean batchSyncEnabled;
    @ConfigProperty(
        name = "cos.connectors.status.sync.realtime.enabled",
        defaultValue = "true")
    boolean realtimeSyncEnabled;
    @ConfigProperty(
        name = "cos.connectors.status.sync.batch.interval")
    String batchSyncInterval;
    @ConfigProperty(
        name = "cos.connectors.status.sync.interval")
    String statusSyncInterval;

    public ConnectorDeploymentStatusSync() {
        this.queue = new ConnectorStatusQueue();
        this.observer = new ConnectorStatusObserver();
    }

    @PostConstruct
    void setUp() {
        this.observer.start();
    }

    @PreDestroy
    void destroy() {
        this.observer.close();
    }

    public void submit(ManagedConnector connector) {
        this.queue.submit(new ConnectorStatusEvent(connector.getMetadata().getName()));
    }

    @Scheduled(
        identity = "cos.connectors.status.sync.batch",
        every = "{cos.connectors.status.sync.batch.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void updateAllConnectorDeploymentStatus() {
        if (!operator.isRunning() || !batchSyncEnabled) {
            return;
        }

        this.queue.submit(new ConnectorStatusEvent(null));

        LOGGER.debug("Sync connectors status interval={}, queue_size={}", batchSyncInterval, this.queue.size());
    }

    @Scheduled(
        identity = "cos.connectors.status.sync",
        every = "{cos.connectors.status.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void updateConnectorDeploymentStatus() {
        if (!operator.isRunning()) {
            LOGGER.debug("Operator is not yet ready");
            return;
        }

        try {
            final int queueSize = queue.size();
            final Collection<ManagedConnector> connectors = queue.poll();

            LOGGER.debug("Polling ManagedConnector status queue (interval={}, connectors={}, queue_size={})",
                statusSyncInterval,
                connectors.size(),
                queueSize);

            for (ManagedConnector connector : connectors) {
                updateConnectorDeploymentStatus(connector);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateConnectorDeploymentStatus(ManagedConnector connector) {
        if (!operator.isRunning()) {
            return;
        }

        LOGGER.debug("Update connector status (name: {}, phase: {})",
            connector.getMetadata().getName(),
            connector.getStatus().getPhase());

        try {
            ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
            ds.setResourceVersion(connector.getStatus().getDeployment().getDeploymentResourceVersion());

            if (connector.getStatus() == null) {
                ds.setPhase("provisioning");
            } else if (connector.getStatus().getPhase() == null) {
                ds.setPhase("provisioning");
            } else {
                switch (connector.getStatus().getPhase()) {
                    case Augmentation:
                    case Initialization:
                        ds.setPhase("provisioning");
                        break;
                    case Deleted:
                    case Deleting:
                        // TODO: we should distinguish between deleted/deleting
                        ds.setPhase("deleted");
                        break;
                    case Stopping:
                    case Stopped:
                        // TODO: we should distinguish between deleted/deleting
                        ds.setPhase("stopped");
                        break;
                    case Monitor:
                        setConnectorOperators(connector, ds);
                        setConnectorStatus(connector, ds);
                        break;
                    default:
                        throw new IllegalStateException(
                            "Unsupported phase ("
                                + connector.getStatus().getPhase()
                                + ") for connector "
                                + connector.getMetadata().getName());
                }
            }

            if (ds.getPhase() == null) {
                ds.setPhase("provisioning");
            }

            controlPlane.updateConnectorStatus(
                connector,
                ds);

        } catch (MetaClientException e) {
            LOGGER.warn("Error retrieving status for connector " + connector.getMetadata().getName(), e);
        } catch (FleetManagerClientException e) {
            //TODO: remove 404 after https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-manager/issues/2
            if (e.getError() != null && (e.getStatusCode() == 404 || e.getStatusCode() == 410)) {
                LOGGER.info("Connector " + connector.getMetadata().getName() + " does not exists anymore, deleting it");
                fleetShard.deleteManagedConnector(connector);
            } else {
                LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error updating status of connector " + connector.getMetadata().getName(), e);
        }
    }

    private void setConnectorOperators(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        // report available operators
        deploymentStatus.setOperators(
            new ConnectorDeploymentStatusOperators()
                .assigned(OperatorSupport.toConnectorOperator(connector.getStatus().getAssignedOperator()))
                .available(OperatorSupport.toConnectorOperator(connector.getStatus().getAvailableOperator())));
    }

    private void setConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        ConnectorDeploymentStatusRequest sr = new ConnectorDeploymentStatusRequest()
            .managedConnectorId(connector.getMetadata().getName())
            .deploymentId(connector.getSpec().getDeploymentId())
            .connectorId(connector.getSpec().getConnectorId())
            .connectorTypeId(connector.getSpec().getConnectorTypeId());

        for (DeployedResource resource : connector.getStatus().getResources()) {
            // don't include secrets ...
            if (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Secret", resource.getKind())) {
                continue;
            }

            sr.addResourcesItem(
                uc.getAsNode(connector.getMetadata().getNamespace(), resource));
        }

        if (connector.getStatus().getAssignedOperator() != null && sr.getResources() != null) {
            var answer = meta.status(
                connector.getStatus().getAssignedOperator().getMetaService(),
                sr);

            deploymentStatus.setPhase(answer.getPhase());

            // TODO: fix model duplications
            if (answer.getConditions() != null) {
                for (var cond : answer.getConditions()) {
                    deploymentStatus.addConditionsItem(
                        new MetaV1Condition()
                            .type(cond.getType())
                            .status(cond.getStatus())
                            .message(cond.getMessage())
                            .reason(cond.getReason())
                            .lastTransitionTime(cond.getLastTransitionTime()));
                }
            }
        } else {
            deploymentStatus.setPhase("provisioning");
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
            if (!operator.isRunning() || !realtimeSyncEnabled) {
                return;
            }

            queue.submit(new ConnectorStatusEvent(resource.getMetadata().getName()));
        }
    }

    /**
     * Helper class to queue events.
     */
    private class ConnectorStatusQueue {
        private final ReentrantLock lock;
        private final PriorityQueue<ConnectorStatusEvent> queue;

        public ConnectorStatusQueue() {
            this.lock = new ReentrantLock();
            this.queue = new PriorityQueue<>();
        }

        public int size() {
            this.lock.lock();

            try {
                return this.queue.size();
            } finally {
                this.lock.unlock();
            }
        }

        public void submit(ConnectorStatusEvent event) {
            this.lock.lock();

            try {
                this.queue.add(event);
            } finally {
                this.lock.unlock();
            }
        }

        public Collection<ManagedConnector> poll() throws InterruptedException {
            this.lock.lock();

            try {
                ConnectorStatusEvent event = queue.poll();
                if (event == null) {
                    return Collections.emptyList();
                }

                Collection<ManagedConnector> answer;

                if (event.name == null) {
                    answer = fleetShard.lookupManagedConnectors();
                } else {
                    answer = this.queue.stream()
                        .map(ConnectorStatusEvent::getName)
                        .filter(Objects::nonNull)
                        .sorted()
                        .distinct()
                        .flatMap(e -> fleetShard.lookupManagedConnector(e).stream())
                        .collect(Collectors.toList());
                }

                queue.clear();

                LOGGER.debug("ConnectorStatusQueue: event={}, connectors={}", event, answer.size());

                return answer;
            } finally {
                this.lock.unlock();
            }
        }
    }

    /**
     * Helper class to hold a connector update event.
     */
    private class ConnectorStatusEvent implements Comparable<ConnectorStatusEvent> {
        public final String name;

        public ConnectorStatusEvent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public int compareTo(ConnectorStatusEvent o) {
            if (this.name == null && o.name != null) {
                return 1;
            }
            if (this.name == null) {
                return 0;
            }
            if (o.name == null) {
                return -1;
            }

            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return "ConnectorSyncEvent{" +
                "name='" + name + '\'' +
                '}';
        }
    }

}
