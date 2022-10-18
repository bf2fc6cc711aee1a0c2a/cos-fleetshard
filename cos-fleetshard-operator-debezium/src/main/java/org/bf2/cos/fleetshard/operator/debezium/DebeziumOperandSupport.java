package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.debezium.model.ConnectorStatus;
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

    public static KafkaConnectorStatus connectorStatus(KafkaConnector connector) {
        KafkaConnectorStatus status = null;

        if (connector.getStatus().getConnectorStatus() != null
            && connector.getStatus().getConnectorStatus().containsKey("connector")) {
            status = Serialization.jsonMapper().convertValue(
                connector.getStatus().getConnectorStatus().get("connector"),
                KafkaConnectorStatus.class);
        }

        return status;
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

    private static io.fabric8.kubernetes.api.model.Condition cloneCondition(Condition originalCondition,
        String conditionTypePrefix) {
        if (null == conditionTypePrefix) {
            conditionTypePrefix = "";
        }
        var copyiedCondition = new io.fabric8.kubernetes.api.model.Condition();
        copyiedCondition.setReason(originalCondition.getReason());
        copyiedCondition.setMessage(originalCondition.getMessage());
        copyiedCondition.setStatus(originalCondition.getStatus());
        copyiedCondition.setType(conditionTypePrefix + originalCondition.getType());
        copyiedCondition.setLastTransitionTime(originalCondition.getLastTransitionTime());
        return copyiedCondition;
    }

    private static void computeConnectorStatus(
        KafkaConnector kafkaConnector,
        ConnectorStatus connectorStatus) {

        if (null != kafkaConnector) {
            Condition connectorReadyCondition = null;
            Condition connectorNotReadyCondition = null;
            for (Condition condition : kafkaConnector.getStatus().getConditions()) {
                switch (condition.getType()) {
                    case "Ready":
                        connectorReadyCondition = condition;
                        break;
                    case "NotReady":
                        connectorNotReadyCondition = condition;
                        break;
                    default:
                        break;
                }
                connectorStatus.addCondition(cloneCondition(condition, "KafkaConnector:"));
            }

            ConnectorStatusSpec statusSpec = connectorStatus.getStatusSpec();
            var kafkaConnectorStatus = connectorStatus(kafkaConnector);
            io.fabric8.kubernetes.api.model.Condition readyCondition = new io.fabric8.kubernetes.api.model.Condition();
            if (null != kafkaConnectorStatus) {
                if (null != connectorReadyCondition) {
                    readyCondition = cloneCondition(connectorReadyCondition);
                } else {
                    readyCondition.setType("Ready");
                    readyCondition.setLastTransitionTime(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
                readyCondition.setStatus("False");
                switch (kafkaConnectorStatus.getState()) {
                    case KafkaConnectorStatus.STATE_UNASSIGNED:
                        readyCondition.setReason("Unassigned");
                        readyCondition.setMessage("The connector is not yet assigned or rebalancing is in progress.");
                        statusSpec.setConditions(List.of(readyCondition));
                        statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                        return;
                    case KafkaConnectorStatus.STATE_PAUSED:
                        readyCondition.setReason("Paused");
                        readyCondition.setMessage("The connector is paused.");
                        statusSpec.setConditions(List.of(readyCondition));
                        statusSpec.setPhase(ManagedConnector.STATE_STOPPED);
                        return;
                    default:
                        break;
                }
            }

            connectorStatus.setConnectorReady(connectorReadyCondition != null
                && "True".equals(connectorReadyCondition.getStatus())
                && (connectorNotReadyCondition == null || !"True".equals(connectorNotReadyCondition.getStatus())));

            if (connectorStatus.isConnectorReady()) {

                @SuppressWarnings("unchecked")
                List<Map<String, String>> tasks = new ArrayList<Map<String, String>>((List) kafkaConnector.getStatus()
                    .getConnectorStatus().getOrDefault("tasks", new ArrayList<>()));

                ArrayList<String> errors = new ArrayList<>();
                for (Map<String, String> task : tasks) {
                    if ("FAILED".equals(task.get("state"))) {
                        errors.add(task.get("trace"));
                    }
                }

                if (errors.isEmpty()) {
                    connectorStatus.setReadyCondition(cloneCondition(connectorReadyCondition));
                    statusSpec.setPhase(ManagedConnector.STATE_READY);
                } else {
                    readyCondition.setType("Ready");
                    readyCondition.setStatus("False");
                    readyCondition.setReason("DebeziumException");
                    readyCondition.setMessage(String.join("; \n", errors));
                    readyCondition.setLastTransitionTime(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    connectorStatus.setReadyCondition(readyCondition);
                    statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                }
            } else {
                if (null != connectorReadyCondition && "False".equals(connectorReadyCondition.getStatus())
                    && (null == connectorNotReadyCondition || !"True".equals(connectorNotReadyCondition.getStatus()))) {
                    connectorStatus.setReadyCondition(cloneCondition(connectorReadyCondition));
                } else if (null != connectorNotReadyCondition && "True".equals(connectorNotReadyCondition.getStatus())) {
                    connectorStatus.setReadyCondition(cloneAsReadyFalseCondition(connectorNotReadyCondition));
                } else if (kafkaConnector.getStatus().getConditions().size() > 0) {
                    connectorStatus
                        .setReadyCondition(cloneAsReadyFalseCondition(kafkaConnector.getStatus().getConditions().get(0)));
                }
                statusSpec.setPhase(ManagedConnector.STATE_FAILED);
            }
        }
    }

    private static void computeKafkaConnectStatus(
        KafkaConnect kafkaConnect,
        ConnectorStatus connectorStatus) {
        if (null != kafkaConnect) {
            Condition kconnectReadyCondition = null;
            Condition kconnectNotReadyCondition = null;

            for (Condition condition : kafkaConnect.getStatus().getConditions()) {
                switch (condition.getType()) {
                    case "Ready":
                        kconnectReadyCondition = condition;
                        break;
                    case "NotReady":
                        kconnectNotReadyCondition = condition;
                        break;
                    default:
                        break;
                }
                connectorStatus.addCondition(cloneCondition(condition, "KafkaConnect:"));
            }

            boolean kafkaConnectReady = kconnectReadyCondition != null && "True".equals(kconnectReadyCondition.getStatus())
                && (kconnectNotReadyCondition == null || !"True".equals(kconnectNotReadyCondition.getStatus()));

            if (connectorStatus.isConnectorReady() && !kafkaConnectReady) {
                if (null != kconnectReadyCondition && !"True".equals(kconnectReadyCondition.getStatus())
                    && (null == kconnectNotReadyCondition || !"True".equals(kconnectNotReadyCondition.getStatus()))) {
                    connectorStatus.setReadyCondition(cloneCondition(kconnectReadyCondition));
                } else if (null != kconnectNotReadyCondition && "True".equals(kconnectNotReadyCondition.getStatus())) {
                    connectorStatus.setReadyCondition(cloneAsReadyFalseCondition(kconnectNotReadyCondition));
                    if ("TimeoutException".equals(kconnectNotReadyCondition.getReason())) {
                        io.fabric8.kubernetes.api.model.Condition readyCondition = connectorStatus.readyCondition();
                        readyCondition.setReason("KafkaClusterUnreachable");
                        readyCondition.setMessage("The configured Kafka Cluster is unreachable or ACLs deny access.");
                    }
                }
                connectorStatus.getStatusSpec().setPhase(ManagedConnector.STATE_FAILED);
            } else {
                if (null == connectorStatus.readyCondition()) {
                    io.fabric8.kubernetes.api.model.Condition readyCondition = new io.fabric8.kubernetes.api.model.Condition();
                    readyCondition.setType("Ready");
                    readyCondition.setStatus("False");
                    readyCondition.setReason("Transitioning");
                    readyCondition.setMessage("The connector is transitioning into another state.");
                    readyCondition.setLastTransitionTime(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    connectorStatus.setReadyCondition(readyCondition);
                    connectorStatus.getStatusSpec().setPhase(ManagedConnector.STATE_PROVISIONING);
                }
            }
        }
    }

    public static void computeStatus(ConnectorStatusSpec statusSpec, KafkaConnect kafkaConnect, KafkaConnector connector) {
        var managedConnectorStatus = new ConnectorStatus(statusSpec);
        computeConnectorStatus(connector, managedConnectorStatus);
        if (null == managedConnectorStatus.isConnectorReady()) {
            return;
        }
        computeKafkaConnectStatus(kafkaConnect, managedConnectorStatus);
        managedConnectorStatus.addCondition(managedConnectorStatus.readyCondition());
        statusSpec.setConditions(managedConnectorStatus.getStatusSpecConditions());
    }
}
