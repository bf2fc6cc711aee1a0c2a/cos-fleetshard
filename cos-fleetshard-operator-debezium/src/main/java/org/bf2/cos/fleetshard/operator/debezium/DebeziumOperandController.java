package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;
import org.bf2.cos.fleetshard.operator.debezium.model.KeyAndValueConverters;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.strimzi.api.kafka.model.ClientTlsBuilder;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.KafkaConnectSpecBuilder;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.KafkaConnectorSpecBuilder;
import io.strimzi.api.kafka.model.PasswordSecretSourceBuilder;
import io.strimzi.api.kafka.model.authentication.KafkaClientAuthenticationPlainBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationBuilder;
import io.strimzi.api.kafka.model.connect.ExternalConfigurationVolumeSourceBuilder;
import io.strimzi.api.kafka.model.template.KafkaConnectTemplateBuilder;
import io.strimzi.api.kafka.model.template.PodTemplate;
import io.strimzi.api.kafka.model.template.PodTemplateBuilder;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.IMAGE_PULL_SECRET;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_CLIENT_SECRET_KEY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RESOURCE_TYPES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_DOMAIN;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.STRIMZI_IO_USE_CONNECTOR_RESOURCES;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.computeStatus;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.connector;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createConfig;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createSecretsData;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.lookupConnector;
import static org.bf2.cos.fleetshard.support.CollectionUtils.asBytesBase64;

@Singleton
public class DebeziumOperandController extends AbstractOperandController<DebeziumShardMetadata, ObjectNode, DebeziumDataShape> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumOperandController.class);

    private final DebeziumOperandConfiguration configuration;

    public DebeziumOperandController(KubernetesClient kubernetesClient, DebeziumOperandConfiguration configuration) {
        super(kubernetesClient, DebeziumShardMetadata.class, ObjectNode.class, DebeziumDataShape.class);

        this.configuration = configuration;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return RESOURCE_TYPES;
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnector connector,
        DebeziumShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, DebeziumDataShape> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        final Map<String, String> secretsData = createSecretsData(connectorConfiguration.getConnectorSpec());

        final Secret secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX)
                .build())
            .addToData(EXTERNAL_CONFIG_FILE, asBytesBase64(secretsData))
            .addToData(KAFKA_CLIENT_SECRET_KEY, serviceAccountSpec.getClientSecret())
            .build();

        PodTemplate podTemplate = new PodTemplateBuilder()
            .withImagePullSecrets(IMAGE_PULL_SECRET)
            .build();
        podTemplate.setAdditionalProperty("restartPolicy", "Never");

        final KafkaConnectSpecBuilder kcsb = new KafkaConnectSpecBuilder()
            .withReplicas(1)
            .withBootstrapServers(connector.getSpec().getDeployment().getKafka().getUrl())
            .withKafkaClientAuthenticationPlain(new KafkaClientAuthenticationPlainBuilder()
                .withUsername(serviceAccountSpec.getClientId())
                .withPasswordSecret(new PasswordSecretSourceBuilder()
                    .withSecretName(secret.getMetadata().getName())
                    .withPassword(KAFKA_CLIENT_SECRET_KEY)
                    .build())
                .build())
            .addToConfig(DebeziumConstants.DEFAULT_CONFIG_OPTIONS)
            // add external configuration
            .addToConfig(new TreeMap<>(configuration.kafkaConnect().config()))
            .addToConfig("group.id", connector.getMetadata().getName())
            // converters
            .addToConfig(
                KeyAndValueConverters.getConfig(connectorConfiguration.getDataShapeSpec(), connector, serviceAccountSpec))
            // topics
            .addToConfig("offset.storage.topic", connector.getMetadata().getName() + "-offset")
            .addToConfig("config.storage.topic", connector.getMetadata().getName() + "-config")
            .addToConfig("status.storage.topic", connector.getMetadata().getName() + "-status")
            // added to trigger a re-deployment if the secret changes
            .addToConfig("connector.secret.name", secret.getMetadata().getName())
            .addToConfig("connector.secret.checksum", Secrets.computeChecksum(secret))
            .withTls(new ClientTlsBuilder()
                .withTrustedCertificates(Collections.emptyList())
                .build())
            .withTemplate(new KafkaConnectTemplateBuilder().withPod(podTemplate).build())
            .withExternalConfiguration(new ExternalConfigurationBuilder()
                .addToVolumes(new ExternalConfigurationVolumeSourceBuilder()
                    .withName(EXTERNAL_CONFIG_DIRECTORY)
                    .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(secret.getMetadata().getName())
                        .build())
                    .build())
                .build());

        kcsb.withImage(shardMetadata.getContainerImage());

        final KafkaConnect kc = new KafkaConnectBuilder()
            .withApiVersion(Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnect.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .addToAnnotations(STRIMZI_IO_USE_CONNECTOR_RESOURCES, "true")
                .build())
            .withSpec(kcsb.build())
            .build();

        final KafkaConnector kctr = new KafkaConnectorBuilder()
            .withApiVersion(Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnector.CONSUMED_VERSION)
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .addToLabels(STRIMZI_DOMAIN + "cluster", connector.getMetadata().getName())
                .build())
            .withSpec(new KafkaConnectorSpecBuilder()
                .withClassName(shardMetadata.getConnectorClass())
                .withTasksMax(1)
                .withPause(false)
                .withConfig(createConfig(configuration, connectorConfiguration.getConnectorSpec()))
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

            getKubernetesClient()
                .resources(KafkaConnector.class)
                .inNamespace(connector.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName())
                .patch(kc);

            return false;
        }).orElse(true);
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        Boolean kctr = Resources.delete(
            getKubernetesClient(),
            KafkaConnector.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());
        Boolean kc = Resources.delete(
            getKubernetesClient(),
            KafkaConnect.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());
        Boolean secret = Resources.delete(
            getKubernetesClient(),
            Secret.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX);

        LOGGER.debug("deleting connector {}/{} (KafkaConnector: {}, KafkaConnect: {}, Secret: {})",
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            kctr,
            kc,
            secret);

        return kctr && kc && secret;
    }
}
