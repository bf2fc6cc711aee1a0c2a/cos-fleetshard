package org.bf2.cos.fleetshard.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

@ApplicationScoped
public class FleetShardSyncProducers {

    @Singleton
    @Produces
    public ManagedConnectorCluster operator(
        @ConfigProperty(name = "cos.cluster.id") String clusterId) {

        return new ManagedConnectorClusterBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + clusterId)
                .addToLabels(Resources.LABEL_CLUSTER_ID, clusterId)
                .build())
            .withSpec(new ManagedConnectorClusterSpecBuilder()
                .withClusterId(clusterId)
                .build())
            .build();
    }
}
