package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.debezium.model.KafkaConnectorStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.status.Condition;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.EXTERNAL_CONFIG_FILE;

public class DebeziumOperandSupport {
    public static final String AV_KAFKA_CONNECT = Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnect.CONSUMED_VERSION;
    public static final String AV_KAFKA_CONNECTOR = Constants.RESOURCE_GROUP_NAME + "/" + KafkaConnector.CONSUMED_VERSION;

    public static boolean isSecret(HasMetadata ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("Secret", ref.getKind());
    }

    public static boolean isConfigMap(HasMetadata ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("ConfigMap", ref.getKind());
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

        // add external configuration
        if (configuration.kafkaConnector().config() != null) {
            config.putAll(configuration.kafkaConnector().config());
        }

        if (connectorSpec != null) {
            var cit = connectorSpec.fields();
            while (cit.hasNext()) {
                final var property = cit.next();

                if (!property.getValue().isObject()) {
                    config.put(
                        property.getKey(),
                        property.getValue().asText());
                } else {
                    config.putIfAbsent(
                        property.getKey(),
                        "${file:/opt/kafka/external-configuration/"
                            + EXTERNAL_CONFIG_DIRECTORY
                            + "/"
                            + EXTERNAL_CONFIG_FILE
                            + ":" + property.getKey() + "}");
                }
            }
        }

        return config;
    }

    public static Map<String, String> createSecretsData(JsonNode connectorSpec) {
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
                        new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8));
                }
            }
        }

        return props;
    }

    public static Optional<KafkaConnector> lookupConnector(KubernetesClient client, ManagedConnector connector) {
        return Optional.ofNullable(
            client.resources(KafkaConnector.class)
                .inNamespace(connector.getMetadata().getNamespace())
                .withName(connector.getMetadata().getName())
                .get());
    }

    public static Optional<KafkaConnect> lookupKafkaConnect(KubernetesClient client, ManagedConnector connector) {
        return Optional.ofNullable(
            client.resources(KafkaConnect.class)
                .inNamespace(connector.getMetadata().getNamespace())
                .withName(connector.getMetadata().getName())
                .get());
    }

    private static io.fabric8.kubernetes.api.model.Condition cloneAsReadyFalseCondition(Condition originalCondition) {
        var readyCondition = cloneCondition(originalCondition, "");
        readyCondition.setType("Ready");
        readyCondition.setStatus("False");
        return readyCondition;
    }

    private static io.fabric8.kubernetes.api.model.Condition cloneCondition(Condition originalCondition) {
        return cloneCondition(originalCondition, "");
    }

    private static io.fabric8.kubernetes.api.model.Condition cloneCondition(
        Condition originalCondition,
        String conditionTypePrefix) {
        var copyiedCondition = new io.fabric8.kubernetes.api.model.Condition();
        copyiedCondition.setReason(originalCondition.getReason());
        copyiedCondition.setMessage(originalCondition.getMessage());
        copyiedCondition.setStatus(originalCondition.getStatus());
        copyiedCondition.setType(conditionTypePrefix + originalCondition.getType());
        copyiedCondition.setLastTransitionTime(originalCondition.getLastTransitionTime());
        return copyiedCondition;
    }

    public static void computeStatus(ConnectorStatusSpec statusSpec, KafkaConnect kafkaConnect, KafkaConnector kafkaConnector) {
        statusSpec.setConditions(new ArrayList<>());

        statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);

        var readyCondition = new io.fabric8.kubernetes.api.model.Condition();
        readyCondition.setType("Ready");
        readyCondition.setStatus("False");
        readyCondition.setReason("Transitioning");

        boolean kafkaConnectorFailed = true;

        if (null != kafkaConnector) {
            for (Condition condition : kafkaConnector.getStatus().getConditions()) {

                var rc = cloneCondition(condition, "KafkaConnector:");

                switch (condition.getType()) {
                    case "Ready":
                        readyCondition = cloneCondition(condition);
                        if ("True".equals(condition.getStatus())) {
                            statusSpec.setPhase(ManagedConnector.STATE_READY);
                            kafkaConnectorFailed = false;
                        }
                        break;
                    case "NotReady":
                        if ("ConnectRestException".equals(condition.getReason())) {
                            readyCondition = cloneAsReadyFalseCondition(condition);
                            statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                            break;
                        }
                        break;
                    default:
                        break;
                }

                statusSpec.getConditions().add(rc);
            }
        }

        if (null != kafkaConnect) {
            for (Condition condition : kafkaConnect.getStatus().getConditions()) {

                var rc = cloneCondition(condition, "KafkaConnect:");

                switch (condition.getType()) {
                    case "Ready":
                        if (!"True".equals(condition.getStatus())) {
                            if (!kafkaConnectorFailed) {
                                readyCondition = cloneCondition(condition);
                            }
                            statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                        }
                        break;
                    case "NotReady":
                        if ("TimeoutException".equals(condition.getReason())) {
                            statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                            if (!kafkaConnectorFailed) {
                                readyCondition = cloneAsReadyFalseCondition(condition);
                                readyCondition.setReason("KafkaClusterUnreachable");
                                readyCondition.setMessage("The configured Kafka Cluster is unreachable or ACLs deny access.");
                            }
                            break;
                        }
                        break;
                    default:
                        break;
                }

                statusSpec.getConditions().add(rc);
            }
        }

        statusSpec.getConditions().add(readyCondition);

        if (null != kafkaConnector) {
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
                        default:
                            break;
                    }
                });
        }
    }
}
