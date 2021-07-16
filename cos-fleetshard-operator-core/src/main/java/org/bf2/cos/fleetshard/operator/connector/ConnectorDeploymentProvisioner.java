package org.bf2.cos.fleetshard.operator.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.support.ResourceUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorDeploymentProvisioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentProvisioner.class);

    private final ConnectorDeploymentProvisioner.ConnectorDeploymentQueue queue = new ConnectorDeploymentProvisioner.ConnectorDeploymentQueue();

    @Inject
    FleetShardClient fleetShard;
    @Inject
    FleetManagerClient fleetManager;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.connectors.sync.interval")
    String connectorsSyncInterval;

    public int size() {
        return this.queue.size();
    }

    public void submit(ManagedConnectorCluster cluster, ConnectorDeployment deployment) {
        this.queue.submit(new ConnectorDeploymentEvent(cluster, deployment, false));
    }

    public void run() {
        try {
            final int queueSize = queue.size();
            final Collection<ConnectorDeploymentEvent> deploymentEvents = queue.poll();

            LOGGER.debug("Polling ConnectorDeployment queue (interval={}, deployments={}, queue_size={})",
                connectorsSyncInterval,
                deploymentEvents.size(),
                queueSize);

            for (ConnectorDeploymentEvent event : deploymentEvents) {
                provision(event.getCluster(), event.getDeployment(), event.isRecreate());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void provision(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment,
        final boolean recreate) {
        LOGGER.info("Got connector_id: {}, deployment_id: {}",
            deployment.getSpec().getConnectorId(),
            deployment.getId());

        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = connectorCluster.getSpec().getConnectorsNamespace();
        final String mcId = "c" + UUID.randomUUID().toString().replaceAll("-", "");

        Supplier<ManagedConnector> createConnector = () -> {
            LOGGER.info("Creating connector (connector_id: {}, deployment_id: {})",
                deployment.getSpec().getConnectorId(),
                deployment.getId());

            // TODO: find suitable operator and label according
            return new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(mcId)
                    .withNamespace(connectorsNs)
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
                    .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withClusterId(connectorCluster.getSpec().getId())
                    .withConnectorId(connectorId)
                    .withConnectorTypeId(deployment.getSpec().getConnectorTypeId())
                    .withDeploymentId(deployment.getId())
                    .build())
                .build();
        };

        ManagedConnector connector = recreate ? createConnector.get()
            : fleetShard.lookupManagedConnector(connectorCluster, deployment).orElseGet(() -> {
                LOGGER.info("Connector (connector_id: {}, deployment_id: {}) not found, creating new connector",
                    deployment.getSpec().getConnectorId(),
                    deployment.getId());
                return createConnector.get();
            });

        final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
        final Long drv = deployment.getMetadata().getResourceVersion();

        if (!recreate && Objects.equals(cdrv, drv)) {
            LOGGER.info(
                "Skipping as deployment resource version has not changed (deployment_id={}, version={})",
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());
            return;
        }

        ArrayNode operatorsMeta = deployment.getSpec().getShardMetadata().withArray("operators");
        if (operatorsMeta.size() != 1) {
            throw new IllegalArgumentException("Multiple selectors are not yet supported");
        }

        OperatorSelector operatorSelector = new OperatorSelector(
            deployment.getSpec().getOperatorId(),
            operatorsMeta.get(0).requiredAt("/type").asText(),
            operatorsMeta.get(0).requiredAt("/version").asText());

        connector.getSpec().getDeployment().setResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState());
        connector.getSpec().setOperatorSelector(operatorSelector);

        LOGGER.info((recreate ? "Recreating " : "Provisioning ") + "connector id={} rv={} - {}/{}: {}",
            mcId,
            deployment.getMetadata().getResourceVersion(),
            connectorsNs,
            connectorId,
            Serialization.asJson(deployment));

        kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNs)
            .createOrReplace(connector);
    }

    /**
     * Helper class to queue events.
     */
    private class ConnectorDeploymentQueue {
        private final ReentrantLock lock;
        private final PriorityQueue<ConnectorDeploymentProvisioner.ConnectorDeploymentEvent> queue;

        public ConnectorDeploymentQueue() {
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

        public void submit(ConnectorDeploymentProvisioner.ConnectorDeploymentEvent event) {
            this.lock.lock();

            try {
                this.queue.add(event);
            } finally {
                this.lock.unlock();
            }
        }

        public Collection<ConnectorDeploymentEvent> poll() throws InterruptedException {
            this.lock.lock();

            try {
                final ConnectorDeploymentProvisioner.ConnectorDeploymentEvent event = queue.peek();
                if (event == null) {
                    return Collections.emptyList();
                }

                final Collection<ConnectorDeploymentEvent> answer;

                if (event.deployment == null) {
                    answer = new ArrayList<>();
                    fleetShard.lookupManagedConnectorCluster()
                        .filter(cluster -> cluster.getStatus().isReady())
                        .ifPresentOrElse(
                            cluster -> {
                                LOGGER.debug("Polling to re-sync all fleet manager connectors");

                                final String clusterId = cluster.getSpec().getId();
                                final String connectorsNamespace = cluster.getSpec().getConnectorsNamespace();

                                fleetManager.getDeployments(clusterId, connectorsNamespace).stream()
                                    .map(d -> new ConnectorDeploymentEvent(cluster, d, true))
                                    .collect(Collectors.toCollection(() -> answer));
                            },
                            () -> LOGGER.debug("Operator not yet configured")); // this should never happen
                } else {
                    answer = this.queue.stream()
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
                }

                queue.clear();

                LOGGER.debug("ConnectorDeploymentQueue: event={}, connectors={}", event, answer.size());

                return answer;
            } finally {
                this.lock.unlock();
            }
        }
    }

    /**
     * Helper class to hold a connector update event.
     */
    private static class ConnectorDeploymentEvent
        implements Comparable<ConnectorDeploymentProvisioner.ConnectorDeploymentEvent> {

        private final ManagedConnectorCluster cluster;
        private final ConnectorDeployment deployment;
        private final boolean recreate;

        public ConnectorDeploymentEvent(ManagedConnectorCluster cluster, ConnectorDeployment deployment, boolean recreate) {
            this.cluster = cluster;
            this.deployment = deployment;
            this.recreate = recreate;
        }

        public ManagedConnectorCluster getCluster() {
            return cluster;
        }

        public ConnectorDeployment getDeployment() {
            return deployment;
        }

        public boolean isRecreate() {
            return recreate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConnectorDeploymentEvent that = (ConnectorDeploymentEvent) o;
            return recreate == that.recreate &&
                Objects.equals(cluster, that.cluster) &&
                Objects.equals(deployment, that.deployment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cluster, deployment, recreate);
        }

        @Override
        public int compareTo(ConnectorDeploymentProvisioner.ConnectorDeploymentEvent o) {
            if (this.deployment == null) {
                if (o.deployment != null) {
                    return -1;
                } else {
                    return 0;
                }
            } else if (o.deployment == null) {
                return 1;
            }

            // TODO: can there be an NPE here?
            return this.deployment.getId().compareTo(o.getDeployment().getId());
        }

        @Override
        public String toString() {
            return "ConnectorDeploymentEvent{" +
                "deploymentId='" + (deployment != null ? deployment.getId() : "null") + '\'' +
                '}';
        }
    }

}
