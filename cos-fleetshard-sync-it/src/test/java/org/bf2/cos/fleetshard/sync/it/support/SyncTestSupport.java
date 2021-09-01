package org.bf2.cos.fleetshard.sync.it.support;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;

public class SyncTestSupport {
    @KubernetesTestServer
    protected KubernetesServer ksrv;

    @ConfigProperty(name = "cluster-id")
    protected String clusterId;

    @ConfigProperty(name = "kubernetes.namespace")
    protected String namespace;

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

    public static ConnectorDeployment createDeployment(
        long deploymentRevision,
        Supplier<JsonNode> connectorSpec,
        Supplier<JsonNode> connectorMeta) {

        final String deploymentId = "did";
        final String connectorId = "cid";
        final String connectorTypeId = "ctid";

        return new ConnectorDeployment()
            .kind("ConnectorDeployment")
            .id(deploymentId)
            .metadata(new ConnectorDeploymentAllOfMetadata()
                .resourceVersion(deploymentRevision))
            .spec(new ConnectorDeploymentSpec()
                .connectorId(connectorId)
                .connectorTypeId(connectorTypeId)
                .connectorResourceVersion(1L)
                .kafka(new KafkaConnectionSettings()
                    .bootstrapServer("kafka.acme.com:2181")
                    .clientId(UUID.randomUUID().toString())
                    .clientSecret(toBase64(UUID.randomUUID().toString())))
                .connectorSpec(connectorSpec.get())
                .shardMetadata(connectorMeta.get())
                .desiredState(DESIRED_STATE_READY));
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

    @BeforeEach
    public void setUp() {
        this.fleetShardClient = new FleetShardClient(ksrv.getClient(), clusterId, namespace, namespace, Duration.ZERO);
    }
}
