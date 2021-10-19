package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentSpec;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

public class SyncTestSupport {
    @Inject
    protected KubernetesClient kubernetesClient;
    @Inject
    protected FleetShardSyncConfig config;
    @Inject
    protected FleetShardClient fleetShardClient;

    public static JsonNode deploymentList(ConnectorDeployment... deployments) {
        var items = new ConnectorDeploymentList();
        items.page(1);
        items.size(deployments.length);
        items.total(deployments.length);

        for (ConnectorDeployment deployment : deployments) {
            items.addItemsItem(deployment);
        }

        return Serialization.jsonMapper().convertValue(items, JsonNode.class);
    }

    public static ConnectorDeployment deployment(String name, long revision, Consumer<ConnectorDeploymentSpec> consumer) {
        ConnectorDeployment answer = new ConnectorDeployment()
            .kind("ConnectorDeployment")
            .id(name)
            .metadata(new ConnectorDeploymentAllOfMetadata().resourceVersion(revision))
            .spec(new ConnectorDeploymentSpec());

        consumer.accept(answer.getSpec());

        return answer;
    }

    public static JsonNode node(Consumer<ObjectNode> consumer) {
        ObjectNode answer = Serialization.jsonMapper().createObjectNode();
        consumer.accept(answer);
        return answer;
    }

    public static <T> T until(Callable<Optional<T>> supplier, Predicate<? super T> predicate) {
        return Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(supplier, item -> item.filter(predicate).isPresent())
            .get();
    }

    public static void untilAsserted(ThrowingRunnable runnable) {
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(runnable);
    }
}
