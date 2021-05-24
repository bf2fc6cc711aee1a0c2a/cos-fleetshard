package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.operator.fleet.FleetManager;
import org.bf2.cos.fleetshard.operator.fleet.FleetShard;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the connectors.
 */
@ApplicationScoped
public class ConnectorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSync.class);

    @Inject
    FleetManager controlPlane;
    @Inject
    FleetShard fleetShard;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.connectors.meta.host")
    Optional<String> metaServiceHost;

    @ConfigProperty(
        name = "cos.connectors.meta.mode",
        defaultValue = "ephemeral")
    String metaServiceMode;

    @Timed(
        value = "cos.connectors.sync.poll",
        extraTags = { "resource", "ManagedConnectors" },
        description = "The time spent processing polling calls")
    @Counted(
        value = "cos.connectors.sync.poll",
        extraTags = { "resource", "ManagedConnectors" },
        description = "The number of polling calls")
    @Scheduled(
        every = "{cos.connectors.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.debug("Sync connectors");

        ManagedConnectorCluster cluster = fleetShard.lookupManagedConnectorCluster(kubernetesClient.getNamespace());
        if (cluster == null
            || cluster.getStatus() == null
            || !Objects.equals(cluster.getStatus().getPhase(), ManagedConnectorClusterStatus.PhaseType.Ready)) {
            LOGGER.debug("Operator not yet configured");
            return;
        }

        LOGGER.debug("Polling for control plane connectors");

        for (var deployment : controlPlane.getDeployments(cluster.getSpec().getId(),
            cluster.getSpec().getConnectorsNamespace())) {
            provision(cluster, deployment);
        }
    }

    private void provision(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        try {
            LOGGER.debug(
                "Polling for control plane connectors {}",
                Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(deployment));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        if (deployment.getSpec().getShardMetadata() == null) {
            LOGGER.warn("ShardMetadata is empty !!!");
        }

        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = connectorCluster.getSpec().getConnectorsNamespace();
        final String mcId = "c" + UUID.randomUUID().toString().replaceAll("-", "");
        final String image = deployment.getSpec().getShardMetadata().requiredAt("/meta_image").asText();

        String metaHost = metaServiceHost.orElse(null);

        //
        // If the meta service mode is "deployment", create a deployment based on the image name
        //
        if ("deployment".equals(metaServiceMode)) {
            Service metaService = fleetShard.lookupMetaService(connectorCluster, deployment);

            if (metaService == null) {
                String name = "m" + UUID.randomUUID().toString().replaceAll("-", "");

                var md = ConnectorSupport.createMetaDeployment(
                    connectorCluster,
                    connectorsNs,
                    name,
                    image);
                var ms = ConnectorSupport.createMetaDeploymentService(
                    connectorCluster,
                    connectorsNs,
                    name,
                    image);

                try {
                    LOGGER.info("md: {}", Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(md));
                    LOGGER.info("ms: {}", Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ms));
                } catch (Exception e) {
                    // ignore
                }

                // TODO: set-up ssl
                kubernetesClient.apps().deployments()
                    .inNamespace(connectorsNs)
                    .create(md);
                kubernetesClient.services()
                    .inNamespace(connectorsNs)
                    .create(ms);

                metaHost = name;
            } else {
                metaHost = metaService.getMetadata().getName();
            }
        }

        //
        //Create or update the connector
        //

        ManagedConnector connector = fleetShard.lookupManagedConnector(connectorCluster, deployment);

        if (connector == null) {
            // TODO: find suitable operator and label according
            connector = new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(mcId)
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
                    .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withConnectorId(connectorId)
                    .withConnectorTypeId(deployment.getSpec().getConnectorTypeId())
                    .withDeploymentId(deployment.getId())
                    .build())
                .build();
        } else {
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

        connector.getSpec().getDeployment().setResourceVersion(deployment.getSpec().getConnectorResourceVersion());
        connector.getSpec().getDeployment().setDeploymentResourceVersion(deployment.getMetadata().getResourceVersion());
        connector.getSpec().getDeployment().setKafkaId(deployment.getSpec().getKafkaId());
        connector.getSpec().getDeployment().setMetaImage(image);
        connector.getSpec().getDeployment().setMetaServiceHost(metaHost);
        connector.getSpec().getDeployment().setDesiredState(deployment.getSpec().getDesiredState());

        LOGGER.info("provisioning connector id={} - {}/{}: {}", mcId, connectorsNs, connectorId, deployment.getSpec());

        kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNs)
            .createOrReplace(connector);
    }

    @Timed(
        value = "cos.connectors.prune",
        extraTags = { "resource", "ManagedConnectors" },
        description = "The time spent processing polling calls")
    @Counted(
        value = "cos.connectors.prune",
        extraTags = { "resource", "ManagedConnectorsAgent" },
        description = "The number of polling calls")
    @Scheduled(
        every = "{cos.connectors.prune.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void prune() {
        LOGGER.debug("Prune (no-op)");

        // TODO: remove deleted connectors
        // TODO: remove unused meta services
    }
}
