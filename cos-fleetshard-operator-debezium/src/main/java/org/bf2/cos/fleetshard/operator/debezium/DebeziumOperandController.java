package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
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
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_PASSWORD_SECRET_KEY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RESOURCE_TYPES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_DOMAIN;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_IO_USE_CONNECTOR_RESOURCES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.computeStatus;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.connector;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createConfig;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createSecret;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.lookupConnector;

@Singleton
public class DebeziumOperandController extends AbstractOperandController<DebeziumShardMetadata, ObjectNode> {
    private final DebeziumOperandConfiguration configuration;

    public DebeziumOperandController(UnstructuredClient uc, DebeziumOperandConfiguration configuration) {
        super(uc, DebeziumShardMetadata.class, ObjectNode.class);

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
                .addToAnnotations(STRIMZI_IO_USE_CONNECTOR_RESOURCES, "true")
                .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_CONNECTOR)
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
                                connector.getDeploymentId()))
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
                .addToLabels(STRIMZI_DOMAIN + "cluster", name)
                .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_CONNECTOR)
                .build())
            .withSpec(new KafkaConnectorSpecBuilder()
                .withClassName(shardMetadata.getConnectorClass())
                .withTasksMax(1)
                .withPause(false)
                .withConfig(createConfig(configuration, connectorSpec))
                .build())
            .build();

        return List.of(secret, kc, kctr);
    }

    @Override
    public void status(ManagedConnector connector) {
        lookupConnector(getKubernetesClient(), connector)
            .filter(kc -> kc.getStatus() != null)
            .ifPresent(kbs -> computeStatus(connector.getStatus().getConnectorStatus(), kbs));
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return lookupConnector(getKubernetesClient(), connector).map(kc -> {
            if (kc.getSpec().getPause() != null && kc.getSpec().getPause()) {
                return connector(kc)
                    .map(c -> KafkaConnectorStatus.STATE_PAUSED.equals(c.state))
                    .orElse(false);
            }

            kc.getSpec().setPause(true);

            getKubernetesClient().resources(KafkaConnector.class).patch(kc);

            return false;
        }).orElse(true);
    }
}
