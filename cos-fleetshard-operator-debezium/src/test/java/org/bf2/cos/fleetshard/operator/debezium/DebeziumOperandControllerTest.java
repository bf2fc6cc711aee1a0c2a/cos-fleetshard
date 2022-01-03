package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaConnectorStatusBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DebeziumOperandControllerTest {
    private static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid";
    private static final Long DEFAULT_CONNECTOR_REVISION = 1L;
    private static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid";
    private static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/pg:1";
    private static final String DEFAULT_DEPLOYMENT_ID = "1";
    private static final Long DEFAULT_DEPLOYMENT_REVISION = 1L;
    private static final String DEFAULT_KAFKA_CLIENT_ID = "kcid";
    private static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181";
    private static final String PG_CLASS = "io.debezium.connector.postgresql.PostgresConnector";
    private static final String PG_ARTIFACT_SHA = "8b4aff3142e0340ece4586d53793bd2108d8f0fa1f87f8";

    private static final DebeziumOperandConfiguration CONFIGURATION = new DebeziumOperandConfiguration() {
        @Override
        public String keyConverter() {
            return "";
        }

        @Override
        public String valueConverter() {
            return "";
        }

        @Override
        public ContainerImage containerImage() {
            return new ContainerImage() {
                @Override
                public String registry() {
                    return "quay.io";
                }

                @Override
                public String group() {
                    return "rhoas";
                }
            };
        }

        @Override
        public KafkaConnect kafkaConnect() {
            return new KafkaConnect() {
                @Override
                public Map<String, String> config() {
                    return Map.of();
                }
            };
        }

        @Override
        public KafkaConnector kafkaConnector() {
            return new KafkaConnector() {
                @Override
                public Map<String, String> config() {
                    return Map.of();
                }
            };
        }
    };

    public static Stream<Arguments> computeStatus() {
        return Stream.of(
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                "Ready",
                "reason",
                ManagedConnector.STATE_READY),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                "NotReady",
                "reason",
                ManagedConnector.STATE_PROVISIONING),
            arguments(
                KafkaConnectorStatus.STATE_RUNNING,
                "NotReady",
                "ConnectRestException",
                ManagedConnector.STATE_FAILED),
            arguments(
                KafkaConnectorStatus.STATE_FAILED,
                "Foo",
                "Bar",
                ManagedConnector.STATE_FAILED),
            arguments(
                KafkaConnectorStatus.STATE_PAUSED,
                "Foo",
                "Bar",
                ManagedConnector.STATE_STOPPED),
            arguments(
                KafkaConnectorStatus.STATE_UNASSIGNED,
                "Foo",
                "Bar",
                ManagedConnector.STATE_PROVISIONING));
    }

    @Test
    void declaresExpectedResourceTypes() {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(kubernetesClient, CONFIGURATION);

        assertThat(controller.getResourceTypes())
            .hasSize(2)
            .anyMatch(ctx -> {
                return Constants.RESOURCE_GROUP_NAME.equals(ctx.getGroup())
                    && KafkaConnect.CONSUMED_VERSION.equals(ctx.getVersion())
                    && KafkaConnect.RESOURCE_KIND.equals(ctx.getKind());
            })
            .anyMatch(ctx -> {
                return Constants.RESOURCE_GROUP_NAME.equals(ctx.getGroup())
                    && KafkaConnector.CONSUMED_VERSION.equals(ctx.getVersion())
                    && KafkaConnector.RESOURCE_KIND.equals(ctx.getKind());
            });
    }

    @Test
    void reify() {
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(kubernetesClient, CONFIGURATION);

        final String kcsB64 = Base64.getEncoder().encodeToString("kcs".getBytes(StandardCharsets.UTF_8));
        final String pwdB64 = Base64.getEncoder().encodeToString("orderpw".getBytes(StandardCharsets.UTF_8));

        var spec = Serialization.jsonMapper().createObjectNode()
            .put("database.hostname", "orderdb")
            .put("database.port", "5432")
            .put("database.user", "orderuser")
            .put("database.dbname", "orderdb")
            .put("database.server.name", "dbserver1")
            .put("schema.include.list", "purchaseorder")
            .put("table.include.list", "purchaseorder.outboxevent")
            .put("tombstones.on.delete", "false")
            .put("key.converter", "org.apache.kafka.connect.storage.StringConverter")
            .put("value.converter", "org.apache.kafka.connect.storage.StringConverter")
            .put("transforms", "saga")
            .put("transforms.saga.type", "io.debezium.transforms.outbox.EventRouter")
            .put("transforms.saga.route.topic.replacement", "${routedByValue}.request")
            .put("poll.interval.ms", "100")
            .put("consumer.interceptor.classes", "io.opentracing.contrib.kafka.TracingConsumerInterceptor")
            .put("producer.interceptor.classes", "io.opentracing.contrib.kafka.TracingProducerInterceptor");

        spec.with("database.password").put("kind", "base64").put("value", pwdB64);

        var resources = controller.doReify(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(DEFAULT_MANAGED_CONNECTOR_ID)
                    .build())
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withConnectorId(DEFAULT_MANAGED_CONNECTOR_ID)
                    .withDeploymentId(DEFAULT_DEPLOYMENT_ID)
                    .withDeployment(new DeploymentSpecBuilder()
                        .withConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID)
                        .withSecret("secret")
                        .withConnectorResourceVersion(DEFAULT_CONNECTOR_REVISION)
                        .withDeploymentResourceVersion(DEFAULT_DEPLOYMENT_REVISION)
                        .withDesiredState(DESIRED_STATE_READY)
                        .build())
                    .build())
                .build(),
            new org.bf2.cos.fleetshard.operator.debezium.DebeziumShardMetadataBuilder()
                .withContainerImage(DEFAULT_CONNECTOR_IMAGE)
                .withConnectorClass(PG_CLASS)
                .withConnectorName("debezium-connector-postgres")
                .withConnectorVersion("1.5.3.Final")
                .withConnectorSha512sum(PG_ARTIFACT_SHA)
                .build(),
            spec,
            new KafkaSpecBuilder()
                .withBootstrapServers(DEFAULT_KAFKA_SERVER)
                .withClientId(DEFAULT_KAFKA_CLIENT_ID)
                .withClientSecret(kcsB64)
                .build());

        assertThat(resources)
            .anyMatch(DebeziumOperandSupport::isKafkaConnect)
            .anyMatch(DebeziumOperandSupport::isKafkaConnector)
            .anyMatch(DebeziumOperandSupport::isSecret);

        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnect)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnect.class, kc -> {
                assertThat(kc.getSpec().getImage()).isEqualTo(DEFAULT_CONNECTOR_IMAGE);
            });
        assertThat(resources)
            .filteredOn(DebeziumOperandSupport::isKafkaConnector)
            .hasSize(1)
            .first()
            .isInstanceOfSatisfying(KafkaConnector.class, kc -> {
                assertThat(kc.getSpec().getConfig()).containsEntry(
                    "database.password",
                    "${file:/opt/kafka/external-configuration/"
                        + DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY
                        + "/"
                        + EXTERNAL_CONFIG_FILE
                        + ":database.password}");
            });

    }

    @ParameterizedTest
    @MethodSource
    void computeStatus(
        String connectorState,
        String conditionType,
        String conditionReason,
        String expectedConnectorState) {

        ConnectorStatusSpec status = new ConnectorStatusSpec();

        DebeziumOperandSupport.computeStatus(
            status,
            new KafkaConnectorBuilder()
                .withStatus(new KafkaConnectorStatusBuilder()
                    .addToConditions(new ConditionBuilder()
                        .withType(conditionType)
                        .withReason(conditionReason)
                        .build())
                    .addToConnectorStatus("connector",
                        new org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatusBuilder()
                            .withState(connectorState)
                            .build())
                    .build())
                .build());

        assertThat(status.getPhase()).isEqualTo(expectedConnectorState);
        assertThat(status.getConditions()).anySatisfy(condition -> {
            assertThat(condition)
                .hasFieldOrPropertyWithValue("type", conditionType)
                .hasFieldOrPropertyWithValue("reason", conditionReason);
        });
    }
}
