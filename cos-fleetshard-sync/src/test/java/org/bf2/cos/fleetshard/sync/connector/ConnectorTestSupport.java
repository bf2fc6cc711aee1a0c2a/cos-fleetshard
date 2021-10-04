package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.mockito.Mockito;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public final class ConnectorTestSupport {
    private ConnectorTestSupport() {
    }

    public static Optional<ManagedConnector> lookupConnector(
        Collection<ManagedConnector> connectors,
        String clusterId,
        ConnectorDeployment deployment) {

        return connectors.stream().filter(
            entry -> {
                return Objects.equals(Connectors.generateConnectorId(deployment.getId()), entry.getMetadata().getName());
            }).findFirst();
    }

    public static Optional<Secret> lookupSecret(
        Collection<Secret> secrets,
        String clusterId,
        ConnectorDeployment deployment) {

        return secrets.stream().filter(
            entry -> {
                return Objects.equals(Secrets.generateConnectorSecretId(deployment.getId()), entry.getMetadata().getName());
            }).findFirst();
    }

    public static ManagedConnectorCluster createCluster() {
        final String clusterId = uid();

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

    public static ConnectorDeployment createDeployment(long deploymentRevision) {
        return createDeployment(
            deploymentRevision,
            () -> {
                ObjectNode answer = Serialization.jsonMapper().createObjectNode();
                answer.with("connector").put("foo", "connector-foo");
                answer.with("kafka").put("topic", "kafka-foo");
                return answer;
            },
            () -> {
                CamelConnectorMeta answer = new CamelConnectorMeta(
                    "sink",
                    "quay.io/mcs_dev/aws-s3-sink:0.0.1",
                    CamelConnectorOperator.of(
                        "camel-connector-operator",
                        "[1.0.0,2.0.0)"),
                    Map.of(
                        "connector", "aws-s3-sink",
                        "kafka", "managed-kafka-source"));

                return Serialization.jsonMapper().convertValue(answer, JsonNode.class);
            });
    }

    public static ConnectorDeployment createDeployment(long deploymentRevision, Consumer<ConnectorDeployment> customizer) {
        ConnectorDeployment answer = createDeployment(deploymentRevision);
        customizer.accept(answer);
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

    @SuppressWarnings("unchecked")
    public static FleetShardClient fleetShard(
        String clusterId,
        Collection<ManagedConnector> connectors,
        Collection<Secret> secrets) {

        Map<String, ManagedConnector> allConnectors = connectors.stream()
            .collect(Collectors.toMap(e -> e.getMetadata().getName(), Function.identity()));
        Map<String, Secret> allSecrets = secrets.stream()
            .collect(Collectors.toMap(e -> e.getMetadata().getName(), Function.identity()));

        FleetShardClient fleetShard = Mockito.mock(FleetShardClient.class);

        when(fleetShard.getClusterId())
            .thenAnswer(invocation -> clusterId);

        when(fleetShard.getConnector(any(ConnectorDeployment.class)))
            .thenAnswer(invocation -> {
                return lookupConnector(allConnectors.values(), clusterId, invocation.getArgument(0));
            });
        when(fleetShard.getSecret(any(ConnectorDeployment.class)))
            .thenAnswer(invocation -> {
                return lookupSecret(allSecrets.values(), clusterId, invocation.getArgument(0));
            });
        when(fleetShard.editConnector(any(String.class), any()))
            .thenAnswer(invocation -> {
                return allConnectors.computeIfPresent(
                    invocation.getArgument(0, String.class),
                    (k, v) -> {
                        invocation.getArgument(1, Consumer.class).accept(v);
                        return v;
                    });
            });

        when(fleetShard.createConnector(any(ManagedConnector.class)))
            .thenAnswer(invocation -> {
                var arg = invocation.getArgument(0, ManagedConnector.class);
                allConnectors.put(arg.getMetadata().getName(), arg);
                return arg;
            });
        when(fleetShard.createSecret(any(Secret.class)))
            .thenAnswer(invocation -> {
                var arg = invocation.getArgument(0, Secret.class);
                allSecrets.put(arg.getMetadata().getName(), arg);
                return arg;
            });

        when(fleetShard.getOrCreateManagedConnectorCluster())
            .thenAnswer(invocation -> {
                return new ManagedConnectorClusterBuilder()
                    .withMetadata(new ObjectMetaBuilder()
                        .withName(Clusters.CONNECTOR_CLUSTER_PREFIX + "-" + clusterId)
                        .addToLabels(Resources.LABEL_CLUSTER_ID, clusterId)
                        .build())
                    .withSpec(new ManagedConnectorClusterSpecBuilder()
                        .withClusterId(clusterId)
                        .build())
                    .build();
            });

        return fleetShard;
    }

    public static class CamelConnectorMeta {
        @JsonProperty("connector_type")
        String connectorType;

        @JsonProperty("connector_image")
        String connectorImage;

        @JsonProperty("operators")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<CamelConnectorOperator> operators;

        @JsonProperty("kamelets")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, String> kamelets;

        public CamelConnectorMeta(
            String connectorType,
            String connectorImage,
            CamelConnectorOperator operator,
            Map<String, String> kamelets) {

            this.connectorType = connectorType;
            this.connectorImage = connectorImage;
            this.operators = List.of(operator);
            this.kamelets = kamelets;
        }
    }

    public static class CamelConnectorOperator {
        @JsonProperty("type")
        String type;

        @JsonProperty("version")
        String version;

        public CamelConnectorOperator(String type, String version) {
            this.type = type;
            this.version = version;
        }

        public static CamelConnectorOperator of(String type, String version) {
            return new CamelConnectorOperator(type, version);
        }
    }
}
