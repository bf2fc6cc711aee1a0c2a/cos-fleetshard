package org.bf2.cos.fleetshard.operator.debezium;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.KafkaConnectSpecBuilder;
import io.strimzi.api.kafka.model.KafkaConnectTlsBuilder;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.KafkaConnectorSpecBuilder;
import io.strimzi.api.kafka.model.PasswordSecretSourceBuilder;
import io.strimzi.api.kafka.model.authentication.KafkaClientAuthenticationPlainBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationVolumeSourceBuilder;
import io.strimzi.api.kafka.model.connect.build.BuildBuilder;
import io.strimzi.api.kafka.model.connect.build.DockerOutputBuilder;
import io.strimzi.api.kafka.model.connect.build.PluginBuilder;
import io.strimzi.api.kafka.model.connect.build.TgzArtifactBuilder;
import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.KafkaConnectorStatus;
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorTaskStatus;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.bf2.cos.fleetshard.support.resources.UnstructuredSupport;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.DELETION_MODE_ANNOTATION;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_PASSWORD_SECRET_KEY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.isKafkaConnector;
import static org.bf2.cos.fleetshard.support.PropertiesUtil.asBytesBase64;

@Singleton
public class DebeziumOperandController extends AbstractOperandController<DebeziumShardMetadata, ObjectNode> {
    private static final List<ResourceDefinitionContext> RESOURCE_TYPES = List.of(
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.STRIMZI_GROUP)
            .withVersion(KafkaConnect.CONSUMED_VERSION)
            .withKind(KafkaConnect.RESOURCE_KIND)
            .withPlural(KafkaConnect.RESOURCE_PLURAL)
            .build(),
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(Constants.STRIMZI_GROUP)
            .withVersion(KafkaConnector.CONSUMED_VERSION)
            .withKind(KafkaConnector.RESOURCE_KIND)
            .withPlural(KafkaConnector.RESOURCE_PLURAL)
            .build());

    private final UnstructuredClient uc;
    private final DebeziumOperandConfiguration configuration;

    public DebeziumOperandController(UnstructuredClient uc, DebeziumOperandConfiguration configuration) {
        super(DebeziumShardMetadata.class, ObjectNode.class);

        this.uc = uc;
        this.configuration = configuration;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return RESOURCE_TYPES;
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnectorSpec connector,
        DebeziumShardMetadata shardMetadata,
        ObjectNode connectorSpec,
        KafkaSpec kafkaSpec) {

        final String connectorId = connector.getId();
        final String name = connectorId + "-dbz";
        final String secretName = name + "-" + EXTERNAL_CONFIG_DIRECTORY;

        final Secret secret = createSecret(secretName, connectorSpec, kafkaSpec);
        final KafkaConnect kc = new KafkaConnectBuilder()
            .withApiVersion(Constants.STRIMZI_GROUP + "/" + KafkaConnect.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .addToAnnotations("strimzi.io/use-connector-resources", "true")
                .addToAnnotations(DELETION_MODE_ANNOTATION, DELETION_MODE_CONNECTOR)
                .build())
            .withSpec(new KafkaConnectSpecBuilder()
                .withReplicas(1)
                .withBootstrapServers(kafkaSpec.getBootstrapServers())
                .withKafkaClientAuthenticationPlain(new KafkaClientAuthenticationPlainBuilder()
                    .withUsername(kafkaSpec.getClientId())
                    .withPasswordSecret(new PasswordSecretSourceBuilder()
                        .withSecretName(secretName)
                        .withPassword(KAFKA_PASSWORD_SECRET_KEY)
                        .build())
                    .build())

                .addToConfig("group.id", connectorId)

                .addToConfig("request.timeout.ms", 20_000)
                .addToConfig("retry.backoff.ms", 500)
                .addToConfig("consumer.request.timeout.ms", 20_000)
                .addToConfig("consumer.retry.backoff.ms", 500)
                .addToConfig("producer.request.timeout.ms", 20_000)
                .addToConfig("producer.retry.backoff.ms", 500)
                .addToConfig("producer.compression.type", "lz4")

                .addToConfig("offset.storage.topic", connectorId + "-offset")
                .addToConfig("config.storage.topic", connectorId + "-config")
                .addToConfig("status.storage.topic", connectorId + "-status")

                .addToConfig("config.storage.replication.factor", 2)
                .addToConfig("offset.storage.replication.factor", 2)
                .addToConfig("status.storage.replication.factor", 2)

                .addToConfig("key.converter.schemas.enable", true)
                .addToConfig("value.converter.schemas.enable", true)

                .addToConfig("config.providers", "file")
                .addToConfig("config.providers.file.class",
                    "org.apache.kafka.common.config.provider.FileConfigProvider")

                .withTls(new KafkaConnectTlsBuilder().withTrustedCertificates(Collections.emptyList()).build())
                .withExternalConfiguration(new ExternalConfigurationBuilder()
                    .addToVolumes(new ExternalConfigurationVolumeSourceBuilder()
                        .withName(EXTERNAL_CONFIG_DIRECTORY)
                        .withSecret(new SecretVolumeSourceBuilder()
                            .withSecretName(secretName)
                            .build())
                        .build())
                    .build())
                .withBuild(new BuildBuilder()
                    .withDockerOutput(new DockerOutputBuilder()
                        .withImage(
                            String.format("%s/%s/cos-debezium:%s",
                                configuration.containerImage().registry(),
                                configuration.containerImage().group(),
                                connectorId))
                        .build())
                    .addToPlugins(new PluginBuilder()
                        .withName("debezium-connector")
                        .addToTgzArtifactArtifacts(new TgzArtifactBuilder()
                            .withUrl(DebeziumArtifactUri.getMavenCentralUri(shardMetadata.getConnectorName(),
                                shardMetadata.getConnectorVersion()))
                            .withSha512sum(shardMetadata.getConnectorSha512sum())
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        final KafkaConnector kctr = new KafkaConnectorBuilder()
            .withApiVersion(Constants.STRIMZI_GROUP + "/" + KafkaConnector.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .addToLabels("strimzi.io/cluster", name)
                .addToAnnotations(DELETION_MODE_ANNOTATION, DELETION_MODE_CONNECTOR)
                .build())
            .withSpec(new KafkaConnectorSpecBuilder()
                .withClassName(shardMetadata.getConnectorClass())
                .withTasksMax(1)
                .withConfig(createConfig(connectorSpec))
                .build())
            .build();

        return List.of(secret, kc, kctr);
    }

    @Override
    public void status(ManagedConnectorStatus connector) {
        if (connector.getResources() == null) {
            return;
        }

        for (ResourceRef ref : connector.getResources()) {
            GenericKubernetesResource resource = uc.get(ref.getNamespace(), ref);

            if (!isKafkaConnector(ref)) {
                continue;
            }

            UnstructuredSupport.getPropertyAs(resource, "status", KafkaConnectorStatus.class).ifPresent(status -> {
                ConnectorStatusSpec statusSpec = new ConnectorStatusSpec();
                statusSpec.setConditions(new ArrayList<>());

                for (Condition condition : status.getConditions()) {
                    switch (condition.getType()) {
                        case "Ready":
                            statusSpec.setPhase(ManagedConnector.STATE_READY);
                            break;
                        case "NotReady":
                            //TODO: something better here, we need to check for errors
                            statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);

                            if ("ConnectRestException".equals(condition.getReason())) {
                                statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                            }
                            break;
                        default:
                            statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                            break;
                    }

                    var rc = new io.fabric8.kubernetes.api.model.Condition();
                    rc.setMessage(condition.getMessage());
                    rc.setReason(condition.getReason());
                    rc.setStatus(condition.getStatus());
                    rc.setType(condition.getType());
                    rc.setLastTransitionTime(condition.getLastTransitionTime());

                    statusSpec.getConditions().add(rc);
                }

                if (status.getConnectorStatus() != null && status.getConnectorStatus().containsKey("tasks")) {
                    List<KafkaConnectorTaskStatus> tasks = JacksonUtil.covertToListOf(
                        status.getConnectorStatus().get("tasks"),
                        KafkaConnectorTaskStatus.class);

                    for (KafkaConnectorTaskStatus task : tasks) {
                        if ("FAILED".equals(task.state)) {
                            statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                            break;
                        }
                    }
                }

                connector.setConnectorStatus(statusSpec);
            });
        }
    }

    private Map<String, Object> createConfig(ObjectNode connectorSpec) {
        Map<String, Object> config = new TreeMap<>();

        if (connectorSpec != null) {
            var cit = connectorSpec.fields();
            while (cit.hasNext()) {
                final var property = cit.next();

                if (!property.getValue().isObject()) {
                    config.put(
                        property.getKey(),
                        property.getValue().asText());
                }
            }
        }

        config.putIfAbsent("key.converter", configuration.keyConverter());
        config.putIfAbsent("value.converter", configuration.valueConverter());

        config.putIfAbsent(
            "database.password",
            "${file:/opt/kafka/external-configuration/"
                + EXTERNAL_CONFIG_DIRECTORY
                + "/"
                + EXTERNAL_CONFIG_FILE
                + ":database.password}");

        return config;
    }

    private Secret createSecret(String secretName, JsonNode connectorSpec, KafkaSpec kafkaSpec) {
        Map<String, String> props = new TreeMap<>();
        if (connectorSpec != null) {
            var cit = connectorSpec.fields();
            while (cit.hasNext()) {
                final var property = cit.next();

                if (property.getValue().isObject()) {
                    JsonNode kind = property.getValue().requiredAt("/kind");
                    JsonNode value = property.getValue().requiredAt("/value");

                    if (!"base64".equals(kind.textValue())) {
                        throw new RuntimeException(
                            "Unsupported field kind " + kind + " (key=" + property.getKey() + ")");
                    }

                    props.put(
                        property.getKey(),
                        new String(Base64.getDecoder().decode(value.asText())));
                }
            }
        }

        return new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(secretName)
                .addToAnnotations(DELETION_MODE_ANNOTATION, DELETION_MODE_CONNECTOR)
                .build())
            .addToData(EXTERNAL_CONFIG_FILE, asBytesBase64(props))
            .addToData(KAFKA_PASSWORD_SECRET_KEY, kafkaSpec.getClientSecret())
            .build();
    }

}
