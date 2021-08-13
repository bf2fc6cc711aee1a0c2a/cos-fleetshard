package org.bf2.cos.fleetshard.operator.debezium;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.status.Condition;
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_PASSWORD_SECRET_KEY;
import static org.bf2.cos.fleetshard.support.PropertiesUtil.asBytesBase64;

public class DebeziumOperandSupport {
    public static final String AV_KAFKA_CONNECT = Constants.STRIMZI_GROUP + "/" + KafkaConnect.CONSUMED_VERSION;
    public static final String AV_KAFKA_CONNECTOR = Constants.STRIMZI_GROUP + "/" + KafkaConnector.CONSUMED_VERSION;

    public static boolean isSecret(HasMetadata ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("Secret", ref.getKind());
    }

    public static boolean isSecret(ResourceRef ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("Secret", ref.getKind());
    }

    public static boolean isKafkaConnect(HasMetadata ref) {
        return Objects.equals(AV_KAFKA_CONNECT, ref.getApiVersion())
            && Objects.equals(KafkaConnect.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnect(ResourceRef ref) {
        return Objects.equals(AV_KAFKA_CONNECT, ref.getApiVersion())
            && Objects.equals(KafkaConnect.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnector(HasMetadata ref) {
        return Objects.equals(AV_KAFKA_CONNECTOR, ref.getApiVersion())
            && Objects.equals(KafkaConnector.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnector(ResourceRef ref) {
        return Objects.equals(AV_KAFKA_CONNECTOR, ref.getApiVersion())
            && Objects.equals(KafkaConnector.RESOURCE_KIND, ref.getKind());
    }

    public static Optional<KafkaConnectorStatus> connector(KafkaConnector connector) {
        KafkaConnectorStatus status = null;

        if (connector.getStatus().getConnectorStatus() != null
            && connector.getStatus().getConnectorStatus().containsKey("connector")) {
            status = Serialization.jsonMapper().convertValue(
                connector.getStatus().getConnectorStatus().get("connector"),
                KafkaConnectorStatus.class);
        }

        return Optional.ofNullable(status);
    }

    public static Map<String, Object> createConfig(DebeziumOperandConfiguration configuration, ObjectNode connectorSpec) {
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

    public static Secret createSecret(String secretName, JsonNode connectorSpec, KafkaSpec kafkaSpec) {
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
                .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_CONNECTOR)
                .build())
            .addToData(EXTERNAL_CONFIG_FILE, asBytesBase64(props))
            .addToData(KAFKA_PASSWORD_SECRET_KEY, kafkaSpec.getClientSecret())
            .build();
    }

    public static Optional<KafkaConnector> lookupConnector(KubernetesClient client, ManagedConnector connector) {
        return Optional.ofNullable(
            client.resources(KafkaConnector.class)
                .inNamespace(connector.getMetadata().getNamespace())
                .withName(connector.getSpec().getId() + "-dbz")
                .get());
    }

    public static void computeStatus(ConnectorStatusSpec statusSpec, KafkaConnector kafkaConnector) {
        statusSpec.setConditions(new ArrayList<>());

        for (Condition condition : kafkaConnector.getStatus().getConditions()) {
            switch (condition.getType()) {
                case "Ready":
                    statusSpec.setPhase(ManagedConnector.STATE_READY);
                    break;
                case "NotReady":
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

        connector(kafkaConnector)
            .map(KafkaConnectorStatus::getState)
            .ifPresent(state -> {
                switch (state) {
                    case KafkaConnectorStatus.STATE_FAILED:
                        statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                        break;
                    case KafkaConnectorStatus.STATE_UNASSIGNED:
                        statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                        break;
                    case KafkaConnectorStatus.STATE_PAUSED:
                        statusSpec.setPhase(ManagedConnector.STATE_STOPPED);
                        break;
                }
            });
    }
}
