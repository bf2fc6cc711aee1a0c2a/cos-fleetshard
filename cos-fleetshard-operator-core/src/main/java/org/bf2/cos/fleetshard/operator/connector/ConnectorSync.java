package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.operator.FleetShardOperator;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.support.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the connectors.
 */
@ApplicationScoped
public class ConnectorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSync.class);

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardOperator operator;

    @Scheduled(
        every = "{cos.connectors.poll.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        if (!operator.isRunning()) {
            return;
        }

        LOGGER.debug("Poll connectors");

        fleetShard.lookupManagedConnectorCluster()
            .filter(cluster -> cluster.getStatus().isReady())
            .ifPresentOrElse(
                this::poll,
                () -> LOGGER.debug("Operator not yet configured"));
    }

    private void poll(ManagedConnectorCluster cluster) {
        LOGGER.debug("Polling for control plane connectors");

        final String id = cluster.getSpec().getId();
        final String ns = cluster.getSpec().getConnectorsNamespace();

        for (var deployment : controlPlane.getDeployments(id, ns)) {
            provision(cluster, deployment);
        }
    }

    private void provision(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        try {
            LOGGER.debug(
                "Provision deployment: {}",
                Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(deployment));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        if (deployment.getSpec().getShardMetadata() == null) {
            //TODO: report error
            LOGGER.warn("ShardMetadata is empty !!!");
        }

        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = connectorCluster.getSpec().getConnectorsNamespace();
        final String mcId = "c" + UUID.randomUUID().toString().replaceAll("-", "");

        ManagedConnector connector = fleetShard.lookupManagedConnector(connectorCluster, deployment).orElseGet(() -> {
            LOGGER.info("Connector not found (connector_id: {}, deployment_id: {}), creating a new one",
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
        });

        final Long cdrv = connector.getSpec().getDeployment().getDeploymentResourceVersion();
        final Long drv = deployment.getMetadata().getResourceVersion();

        if (Objects.equals(cdrv, drv)) {
            LOGGER.info(
                "Skipping as deployment resource version has not changed (deployment_id={}, version={})",
                deployment.getId(),
                deployment.getMetadata().getResourceVersion());
            return;
        }

        connector.getSpec().getDeployment().setResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState());
        connector.getSpec().setOperatorSelector(ConnectorSupport.getOperatorSelector(deployment));

        LOGGER.info("provisioning connector id={} rv={} - {}/{}: {}",
            mcId,
            deployment.getMetadata().getResourceVersion(),
            connectorsNs,
            connectorId,
            deployment.getSpec());

        kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNs)
            .createOrReplace(connector);
    }

    @Scheduled(
        every = "{cos.connectors.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncStatus() {
        if (!operator.isRunning()) {
            return;
        }

        LOGGER.info("Sync connector status");

        fleetShard.lookupConnectors()
            .stream()
            .filter(c -> {
                return !c.getStatus().isInPhase(
                    ManagedConnectorStatus.PhaseType.Deleting,
                    ManagedConnectorStatus.PhaseType.Deleted);
            })
            .forEach(c -> {
                controlPlane.updateConnectorStatus(
                    c,
                    fleetShard.getConnectorDeploymentStatus(c));
            });
    }
}
