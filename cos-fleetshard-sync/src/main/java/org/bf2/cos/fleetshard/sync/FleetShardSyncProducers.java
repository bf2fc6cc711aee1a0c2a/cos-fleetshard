package org.bf2.cos.fleetshard.sync;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.bf2.cos.fleet.manager.model.ConnectorClusterPlatform;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
public class FleetShardSyncProducers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetShardSyncProducers.class);

    @Inject
    KubernetesClient client;
    @Inject
    FleetShardSyncConfig config;

    @Singleton
    @Produces
    public ManagedConnectorCluster operator(
        @ConfigProperty(name = "cos.cluster.id") String clusterId) {

        return new ManagedConnectorClusterBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + clusterId)
                .addToLabels(Resources.LABEL_CLUSTER_ID, clusterId)
                .addToLabels(Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_CLUSTER)
                .build())
            .withSpec(new ManagedConnectorClusterSpecBuilder()
                .withClusterId(clusterId)
                .build())
            .build();
    }

    @Singleton
    @Produces
    public ConnectorClusterPlatform clusterInfo() {
        ConnectorClusterPlatform answer = new ConnectorClusterPlatform().type("Kubernetes");

        try {
            var resources = client.genericKubernetesResources("config.openshift.io/v1", "ClusterVersion").list();

            if (resources != null && resources.getItems() != null && resources.getItems().size() == 1) {
                JsonNode content = resources.getItems().get(0).getAdditionalPropertiesNode();
                JsonNode id = content.at("/spec/clusterID");
                JsonNode version = content.at("/status/desired/version");

                answer.id(id.asText());
                answer.version(version.asText());
                answer.type("OpenShift");
            }
        } catch (KubernetesClientException e) {
            // ignore
        }

        if (answer.getId() != null) {
            LOGGER.info("Cluster info type: {}, id: {}, version: {}", answer.getType(), answer.getId(), answer.getVersion());
        } else {
            LOGGER.info("Unable to determine cluster id");
        }

        return answer;
    }

    @Produces
    @Singleton
    @Unremovable
    public MeterFilter configureAllRegistries() {
        List<Tag> tags = new ArrayList<>();

        config.metrics().recorder().tags().common().forEach((k, v) -> {
            tags.add(Tag.of(k, v));
        });

        return MeterFilter.commonTags(tags);
    }
}
