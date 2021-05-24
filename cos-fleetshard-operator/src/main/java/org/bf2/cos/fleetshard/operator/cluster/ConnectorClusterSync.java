package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.operator.fleet.FleetManager;
import org.bf2.cos.fleetshard.operator.fleet.FleetShard;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterSync.class);

    @Inject
    FleetManager controlPlane;
    @Inject
    FleetShard fleetShard;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String connectorsNamespace;

    @Timed(
        value = "cos.cluster.sync.poll",
        extraTags = { "resource", "ManagedConnectorsAgent" },
        description = "The time spent processing polling calls")
    @Counted(
        value = "cos.cluster.sync.poll",
        extraTags = { "resource", "ManagedConnectorsAgent" },
        description = "The number of polling calls")
    @Scheduled(
        every = "{cos.cluster.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.info("Sync cluster (noop)");

        ManagedConnectorCluster cluster = fleetShard.lookupManagedConnectorCluster(kubernetesClient.getNamespace());
        if (cluster == null) {
            cluster = new ManagedConnectorClusterBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(clusterId)
                    .build())
                .withSpec(new ManagedConnectorClusterSpecBuilder()
                    .withId(clusterId)
                    .withConnectorsNamespace(connectorsNamespace)
                    .build())
                .build();

            kubernetesClient.customResources(ManagedConnectorCluster.class).create(cluster);
        } else {
            controlPlane.updateClusterStatus(cluster);
        }
    }
}
