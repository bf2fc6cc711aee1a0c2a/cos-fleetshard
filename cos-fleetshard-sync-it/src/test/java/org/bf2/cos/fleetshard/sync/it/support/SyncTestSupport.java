package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.assertj.core.api.ThrowingConsumer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ThrowingRunnable;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public class SyncTestSupport {
    @Inject
    protected KubernetesClient kubernetesClient;
    @Inject
    protected FleetShardSyncConfig config;
    @Inject
    protected FleetShardClient fleetShardClient;

    public static ObjectNode deploymentList(ConnectorDeployment... deployments) {
        var items = new ConnectorDeploymentList();
        items.page(1);
        items.size(deployments.length);
        items.total(deployments.length);

        for (ConnectorDeployment deployment : deployments) {
            items.addItemsItem(deployment);
        }

        return Serialization.jsonMapper().convertValue(items, ObjectNode.class);
    }

    public static ObjectNode namespaceList(ConnectorNamespaceDeployment... namespaces) {
        var items = new ConnectorNamespaceDeploymentList();
        items.page(1);
        items.size(namespaces.length);
        items.total(namespaces.length);

        for (ConnectorNamespaceDeployment namespace : namespaces) {
            items.addItemsItem(namespace);
        }

        return Serialization.jsonMapper().convertValue(items, ObjectNode.class);
    }

    public static ConnectorNamespaceDeployment namespace(
        String id,
        String name,
        Consumer<ConnectorNamespaceDeployment> consumer) {

        ConnectorNamespaceDeployment answer = new ConnectorNamespaceDeployment().id(id).name(name);

        consumer.accept(answer);

        if (answer.getStatus() == null) {
            answer.setStatus(new ConnectorNamespaceStatus());
        }
        if (answer.getStatus().getConnectorsDeployed() == null) {
            answer.getStatus().setConnectorsDeployed(0);
        }

        return answer;
    }

    public static ConnectorNamespaceDeployment namespace(String id, String name) {
        ConnectorNamespaceDeployment answer = new ConnectorNamespaceDeployment().id(id).name(name);

        ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
            .id(uid())
            .kind(ConnectorNamespaceTenantKind.ORGANISATION);

        answer.setStatus(new ConnectorNamespaceStatus().state(ConnectorNamespaceState.READY).connectorsDeployed(0));
        answer.setTenant(tenant);
        answer.setExpiration(new Date().toString());

        return answer;
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

    public static ConnectorDeployment deployment(
        String name,
        long revision,
        Consumer<ConnectorDeployment> deploymentConsumer,
        Consumer<ConnectorDeploymentSpec> deploymentSpecConsumer) {

        ConnectorDeployment answer = new ConnectorDeployment()
            .kind("ConnectorDeployment")
            .id(name)
            .metadata(new ConnectorDeploymentAllOfMetadata().resourceVersion(revision))
            .spec(new ConnectorDeploymentSpec());

        deploymentConsumer.accept(answer);
        deploymentSpecConsumer.accept(answer.getSpec());

        return answer;
    }

    public static JsonNode node(Consumer<ObjectNode> consumer) {
        ObjectNode answer = Serialization.jsonMapper().createObjectNode();
        consumer.accept(answer);
        return answer;
    }

    public static <T> T until(Callable<Optional<T>> supplier, Predicate<? super T> predicate) {
        return getConditionFactory()
            .until(supplier, item -> item.filter(predicate).isPresent())
            .get();
    }

    public static <T> Collection<T> untilAny(Callable<Collection<T>> supplier, Predicate<? super T> predicate) {
        return getConditionFactory()
            .until(supplier, item -> item.stream().anyMatch(predicate));
    }

    public static ConditionFactory getConditionFactory() {
        return Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS);
    }

    public static void untilAsserted(ThrowingRunnable runnable) {
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(runnable);
    }

    public static <T> void untilAsserted(Callable<Optional<T>> supplier, ThrowingConsumer<T> consumer) {
        getConditionFactory().untilAsserted(() -> {
            assertThat(supplier.call())
                .isPresent()
                .get()
                .satisfies(consumer);
        });
    }

    public static StringValuePattern jp(String expression, String expected) {
        return matchingJsonPath(expression, equalTo(expected));
    }
}
