package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;
import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;

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
    };

    @Test
    void declaresExpectedResourceTypes() {
        UnstructuredClient uc = Mockito.mock(UnstructuredClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(uc, CONFIGURATION);

        assertThat(controller.getResourceTypes())
            .hasSize(2)
            .anyMatch(ctx -> {
                return Constants.STRIMZI_GROUP.equals(ctx.getGroup())
                    && KafkaConnect.CONSUMED_VERSION.equals(ctx.getVersion())
                    && KafkaConnect.RESOURCE_KIND.equals(ctx.getKind());
            })
            .anyMatch(ctx -> {
                return Constants.STRIMZI_GROUP.equals(ctx.getGroup())
                    && KafkaConnector.CONSUMED_VERSION.equals(ctx.getVersion())
                    && KafkaConnector.RESOURCE_KIND.equals(ctx.getKind());
            });
    }

    @Test
    void reify() {
        UnstructuredClient uc = Mockito.mock(UnstructuredClient.class);
        DebeziumOperandController controller = new DebeziumOperandController(uc, CONFIGURATION);

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
            new ManagedConnectorSpecBuilder()
                .withId(DEFAULT_MANAGED_CONNECTOR_ID)
                .withConnectorId(DEFAULT_MANAGED_CONNECTOR_ID)
                .withDeploymentId(DEFAULT_DEPLOYMENT_ID)
                .withDeployment(new DeploymentSpecBuilder()
                    .withConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID)
                    .withSecret("secret")
                    .withSecretChecksum("TODO")
                    .withConnectorResourceVersion(DEFAULT_CONNECTOR_REVISION)
                    .withDeploymentResourceVersion(DEFAULT_DEPLOYMENT_REVISION)
                    .withDesiredState(DESIRED_STATE_READY)
                    .build())
                .build(),
            new org.bf2.cos.fleetshard.operator.debezium.DebeziumShardMetadataBuilder()
                .withConnectorImage(DEFAULT_CONNECTOR_IMAGE)
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
    }
}
