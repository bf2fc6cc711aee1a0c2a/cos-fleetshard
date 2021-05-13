package org.bf2.cos.fleetshard.operator.connector;

import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.common.ResourceUtil;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.scheduler.Scheduled;

/**
 * Implements the synchronization protocol for the connectors.
 */
@ApplicationScoped
public class ConnectorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSync.class);

    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.connectors.meta.host",
        defaultValue = "")
    String metaServiceHost;

    @ConfigProperty(
        name = "cos.connectors.meta.mode",
        defaultValue = "")
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

        ManagedConnectorCluster connectorCluster = lookupManagedConnectorCluster(kubernetesClient.getNamespace());
        if (connectorCluster == null
            || connectorCluster.getStatus() == null
            || !Objects.equals(connectorCluster.getStatus().getPhase(), ManagedConnectorClusterStatus.PhaseType.Ready)) {
            LOGGER.debug("Operator not yet configured");
            return;
        }

        LOGGER.debug("Polling for control plane connectors");

        for (var deployment : controlPlane.getDeployments()) {
            provision(connectorCluster, deployment);
        }
    }

    private void provision(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        final String connectorId = deployment.getSpec().getConnectorId();
        final String connectorsNs = connectorCluster.getStatus().getConnectorsNamespace();
        final String mcId = "c" + UUID.randomUUID().toString().replaceAll("-", "");

        //
        // If the meta service mode is "deployment", create a deployment based on the image name
        //
        if ("deployment".equals(metaServiceMode) && "".equals(metaServiceHost)) {
            Service metaService = lookupMetaService(connectorCluster, deployment);

            if (metaService == null) {
                String name = "m" + UUID.randomUUID().toString().replaceAll("-", "");

                // TODO: set-up ssl
                kubernetesClient.apps().deployments()
                    .inNamespace(connectorsNs)
                    .create(ConnectorSupport.createMetaDeployment(
                        connectorCluster,
                        connectorsNs,
                        name,
                        deployment.getSpec().getConnectorMetaImage()));
                kubernetesClient.services()
                    .inNamespace(connectorsNs)
                    .create(ConnectorSupport.createMetaDeploymentService(
                        connectorCluster,
                        connectorsNs,
                        name,
                        deployment.getSpec().getConnectorMetaImage()));

                metaServiceHost = name;
            }
        }

        //
        //Create or update the connector
        //

        ManagedConnector connector = lookupManagedConnector(connectorCluster, deployment);

        if (connector == null) {
            connector = new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(mcId)
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
                    .addToOwnerReferences(ResourceUtil.asOwnerReference(connectorCluster))
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withClusterId(connectorCluster.getStatus().getId())
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
        connector.getSpec().getDeployment().setMetaImage(deployment.getSpec().getConnectorMetaImage());
        connector.getSpec().getDeployment().setMetaServiceHost(metaServiceHost);
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

    // ***********************************************
    //
    // Helpers
    //
    // ***********************************************

    private ManagedConnectorCluster lookupManagedConnectorCluster(String namespace) {
        var items = kubernetesClient.customResources(ManagedConnectorCluster.class)
            .inNamespace(namespace)
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors clusters");
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }

    private ManagedConnector lookupManagedConnector(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        var items = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorCluster.getStatus().getConnectorsNamespace())
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors with id " + deployment.getSpec().getConnectorId());
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }

    private Service lookupMetaService(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        var items = kubernetesClient.services()
            .inNamespace(connectorCluster.getStatus().getConnectorsNamespace())
            .withLabel(ManagedConnector.LABEL_CONNECTOR_META, "true")
            .withLabel(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, deployment.getSpec().getConnectorMetaImage())
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple meta service for image " + deployment.getSpec().getConnectorMetaImage());
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }
}
