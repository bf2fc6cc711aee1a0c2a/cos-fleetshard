package org.bf2.cos.fleetshard.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;

/**
 * Implements the synchronization protocol for the agent.
 */
@ApplicationScoped
public class ConnectorClusterSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterSync.class);

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "cos.cluster.id")
    String clusterId;
    @ConfigProperty(
        name = "cos.connectors.namespace")
    String connectorsNamespace;

    @Scheduled(
        every = "{cos.cluster.sync.interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sync() {
        LOGGER.info("Sync cluster");

        final String name = ConnectorClusterSupport.clusterName(clusterId);

        ManagedConnectorCluster cluster = fleetShard.lookupManagedConnectorCluster(kubernetesClient.getNamespace(), name)
            .orElseGet(() -> {
                return kubernetesClient.customResources(ManagedConnectorCluster.class)
                    .create(new ManagedConnectorClusterBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                            .withName(name)
                            .build())
                        .withSpec(new ManagedConnectorClusterSpecBuilder()
                            .withId(clusterId)
                            .withConnectorsNamespace(connectorsNamespace)
                            .build())
                        .build());
            });

        controlPlane.updateClusterStatus(cluster);
    }
}
