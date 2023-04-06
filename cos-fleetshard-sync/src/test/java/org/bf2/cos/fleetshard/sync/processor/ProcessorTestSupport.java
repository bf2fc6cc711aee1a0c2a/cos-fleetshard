package org.bf2.cos.fleetshard.sync.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentAllOfMetadata;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleet.manager.model.ProcessorDeployment;
import org.bf2.cos.fleet.manager.model.ProcessorDeploymentSpec;
import org.bf2.cos.fleet.manager.model.ProcessorDesiredState;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.support.resources.Clusters;
import org.bf2.cos.fleetshard.support.resources.Processors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.mockito.Mockito;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.bf2.cos.fleetshard.support.resources.Secrets.toBase64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public final class ProcessorTestSupport {

    private ProcessorTestSupport() {
    }

    public static Optional<ManagedProcessor> lookupProcessor(
        Collection<ManagedProcessor> processors,
        String clusterId,
        ProcessorDeployment deployment) {

        return processors.stream().filter(
            entry -> {
                return Objects.equals(Processors.generateProcessorId(deployment.getId()), entry.getMetadata().getName());
            }).findFirst();
    }

    public static Optional<Secret> lookupSecret(
        Collection<Secret> secrets,
        String clusterId,
        ProcessorDeployment deployment) {

        return secrets.stream().filter(
            entry -> {
                return Objects.equals(Secrets.generateProcessorSecretId(deployment.getId()), entry.getMetadata().getName());
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

    public static ProcessorDeployment createProcessorDeployment(long deploymentRevision) {
        return createProcessorDeployment(
            deploymentRevision,
            () -> {
                CamelConnectorMeta answer = new CamelConnectorMeta(
                    "sink",
                    "quay.io/mcs_dev/aws-s3-sink:0.0.1",
                    CamelConnectorOperator.of(
                        "camel-connector-operator",
                        "[1.0.0,2.0.0)"),
                    Map.of(
                        "kafka", "managed-kafka-source"));

                return Serialization.jsonMapper().convertValue(answer, JsonNode.class);
            });
    }

    public static ProcessorDeployment createProcessorDeployment(long deploymentRevision,
        Consumer<ProcessorDeployment> customizer) {
        ProcessorDeployment answer = createProcessorDeployment(deploymentRevision);
        customizer.accept(answer);
        return answer;
    }

    public static ProcessorDeployment createProcessorDeployment(
        long deploymentRevision,
        Supplier<JsonNode> connectorMeta) {

        final String namespaceId = "nid";
        final String deploymentId = "did";
        final String processorId = "pid";

        return new ProcessorDeployment()
            .kind("ProcessorDeployment")
            .id(deploymentId)
            .metadata(new ConnectorDeploymentAllOfMetadata()
                .resourceVersion(deploymentRevision))
            .spec(new ProcessorDeploymentSpec()
                .namespaceId(namespaceId)
                .processorId(processorId)
                .processorResourceVersion(1L)
                .kafka(new KafkaConnectionSettings()
                    .url("kafka.acme.com:2181"))
                .serviceAccount(new ServiceAccount()
                    .clientId(UUID.randomUUID().toString())
                    .clientSecret(toBase64(UUID.randomUUID().toString())))
                .shardMetadata(connectorMeta.get())
                .desiredState(ProcessorDesiredState.READY));
    }

    public static FleetShardClient fleetShard(
        String clusterId,
        Collection<ManagedProcessor> processors,
        Collection<Secret> secrets) {

        Map<String, ManagedProcessor> allProcessors = processors.stream()
            .collect(Collectors.toMap(e -> e.getMetadata().getName(), Function.identity()));
        Map<String, Secret> allSecrets = secrets.stream()
            .collect(Collectors.toMap(e -> e.getMetadata().getName(), Function.identity()));

        FleetShardClient answer = Mockito.mock(FleetShardClient.class);

        when(answer.getClusterId())
            .thenAnswer(invocation -> clusterId);

        when(answer.getProcessor(any(ProcessorDeployment.class)))
            .thenAnswer(invocation -> {
                return lookupProcessor(allProcessors.values(), clusterId, invocation.getArgument(0));
            });

        when(answer.getSecret(any(ProcessorDeployment.class)))
            .thenAnswer(invocation -> {
                return lookupSecret(allSecrets.values(), clusterId, invocation.getArgument(0));
            });

        when(answer.createProcessor(any(ManagedProcessor.class)))
            .thenAnswer(invocation -> {
                var arg = invocation.getArgument(0, ManagedProcessor.class);
                allProcessors.put(arg.getMetadata().getName(), arg);
                return arg;
            });
        when(answer.createSecret(any(Secret.class)))
            .thenAnswer(invocation -> {
                var arg = invocation.getArgument(0, Secret.class);
                allSecrets.put(arg.getMetadata().getName(), arg);
                return arg;
            });

        when(answer.getOrCreateManagedConnectorCluster())
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

        return answer;
    }

    public static FleetManagerClient fleetManagerClient() {
        FleetManagerClient answer = Mockito.mock(FleetManagerClient.class);
        return answer;
    }

    public static FleetShardSyncConfig config() {
        FleetShardSyncConfig answer = Mockito.mock(FleetShardSyncConfig.class);
        when(answer.processors()).thenAnswer(invocation -> {
            var processors = Mockito.mock(FleetShardSyncConfig.Processors.class);
            when(processors.annotations()).thenReturn(Collections.emptyMap());
            when(processors.labels()).thenReturn(Collections.emptyMap());
            return processors;
        });
        when(answer.imagePullSecretsName()).thenAnswer(invocation -> {
            return "foo";
        });
        when(answer.namespace()).thenAnswer(invocation -> {
            return "bar";
        });
        when(answer.metrics()).thenAnswer(invocation -> {
            var metrics = Mockito.mock(FleetShardSyncConfig.Metrics.class);
            when(metrics.baseName()).thenReturn("base");
            return metrics;
        });
        when(answer.quota()).thenAnswer(invocation -> {
            return Mockito.mock(FleetShardSyncConfig.Quota.class);
        });
        when(answer.quota().defaultLimits()).thenAnswer(invocation -> {
            return Mockito.mock(FleetShardSyncConfig.DefaultLimits.class);
        });
        when(answer.quota().defaultRequest()).thenAnswer(invocation -> {
            return Mockito.mock(FleetShardSyncConfig.DefaultRequest.class);
        });

        return answer;
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
