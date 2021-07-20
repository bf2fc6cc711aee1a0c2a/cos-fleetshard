package org.bf2.cos.fleetshard.operator.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

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

    public void submit(Long gv) {
        this.queue.submit(gv);
    }

    public void run() {
        fleetShard.lookupManagedConnectorCluster()
            .filter(cluster -> cluster.getStatus().isReady())
            .ifPresentOrElse(
                this::provision,
                () -> LOGGER.debug("Operator not yet configured"));
    }

    private void provision(ManagedConnectorCluster cluster) {
        try {
            final int queueSize = queue.size();
            final Collection<Deployment> deployments = queue.poll();

            LOGGER.debug("Polling ConnectorDeployment queue (interval={}, deployments={}, queue_size={})",
                connectorsSyncInterval,
                deployments.size(),
                queueSize);

            for (Deployment deployment : deployments) {
                provision(cluster, deployment.getDeployment(), deployment.isRecreate());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void provision(
        ManagedConnectorCluster cluster,
        ConnectorDeployment deployment,
        final boolean recreate) {

        LOGGER.info("Got connector_id: {}, deployment_id: {}, deployment_revision: {}, recreate: {}",
            deployment.getSpec().getConnectorId(),
            deployment.getId(),
            deployment.getMetadata().getResourceVersion(),
            recreate);

        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = fleetShard.getConnectorsNamespace();
        final ManagedConnector connector;

        connector = fleetShard.lookupManagedConnector(connectorsNs, deployment).orElseGet(() -> {
            LOGGER.info("Connector not found (connector_id: {}, deployment_id: {}), creating a new one",
                deployment.getSpec().getConnectorId(),
                deployment.getId());

            return createConnector(cluster, deployment);
        });

        if (!recreate) {
            final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
            final Long drv = deployment.getMetadata().getResourceVersion();

            if (Objects.equals(cdrv, drv)) {
                LOGGER.info(
                    "Skipping as deployment resource version has not changed (deployment_id={}, version={})",
                    deployment.getId(),
                    deployment.getMetadata().getResourceVersion());
                return;
            }
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
            connector.getMetadata().getName(),
            deployment.getMetadata().getResourceVersion(),
            connectorsNs,
            connectorId,
            Serialization.asJson(deployment));

        kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNs)
            .createOrReplace(connector);
    }

    private ManagedConnector createConnector(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        return new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName("c" + UUID.randomUUID().toString().replaceAll("-", ""))
                .withNamespace(connectorCluster.getSpec().getConnectorsNamespace())
                .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
                .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(connectorCluster.getSpec().getId())
                .withConnectorId(deployment.getSpec().getConnectorId())
                .withConnectorTypeId(deployment.getSpec().getConnectorTypeId())
                .withDeploymentId(deployment.getId())
                .build())
            .build();
    }

    /**
     * Helper class to queue events.
     */
    private class Deployment {
        private final ConnectorDeployment deployment;
        private final boolean recreate;

        public Deployment(ConnectorDeployment deployment, boolean recreate) {
            this.deployment = deployment;
            this.recreate = recreate;
        }

        public ConnectorDeployment getDeployment() {
            return deployment;
        }

        public boolean isRecreate() {
            return recreate;
        }
    }

    /**
     * Helper class to queue events.
     */
    private class ConnectorDeploymentQueue {
        private final ReentrantLock lock;
        private final Set<Long> events;

        public ConnectorDeploymentQueue() {
            this.lock = new ReentrantLock();
            this.events = new TreeSet<>();
        }

        public int size() {
            this.lock.lock();

            try {
                return this.events.size();
            } finally {
                this.lock.unlock();
            }
        }

        public void submit(Long gv) {
            this.lock.lock();

            try {
                this.events.add(gv);
            } finally {
                this.lock.unlock();
            }
        }

        public Collection<Deployment> poll() throws InterruptedException {
            this.lock.lock();

            try {
                if (events.isEmpty()) {
                    return Collections.emptyList();
                }

                final Collection<Deployment> answer = new ArrayList<>();
                final long gv = Collections.min(events) == 0 ? 0 : Collections.max(events);

                fleetManager.getDeployments(gv).stream()
                    .map(d -> new Deployment(d, gv == 0))
                    .forEach(answer::add);

                events.clear();

                LOGGER.debug("ConnectorDeploymentQueue: gv={}, connectors={}", gv, answer.size());

                return answer;
            } finally {
                this.lock.unlock();
            }
        }
    }

}
