package org.bf2.cos.fleetshard.operator.debezium;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetail;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorStatus;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorTask;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumShardMetadata;
import org.bf2.cos.fleetshard.support.resources.Conditions;
import org.bf2.cos.fleetshard.support.resources.Resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.sundr.utils.Strings;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CLASS_NAME_MYSQL_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CLASS_NAME_POSTGRES_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CLASS_NAME_SQLSERVER_CONNECTOR;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONFIG_OPTION_POSTGRES_PLUGIN_NAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONFIG_OPTION_SQLSERVER_DATABASE_ENCRYPT;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONFIG_OPTION_SQLSERVER_DATABASE_TRUST_CERTIFICATE;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.PLUGIN_NAME_PGOUTPUT;

public final class DebeziumOperandSupport {
    private DebeziumOperandSupport() {
    }

    public static ContainerPort port(int container, String name) {
        ContainerPort port = new ContainerPort();
        port.setContainerPort(container);
        port.setName(name);
        port.setProtocol("TCP");

        return port;
    }

    public static String mapAsString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();

        map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
                sb.append("\n");
            });

        return sb.toString();
    }

    public static Volume volume(Secret source) {
        Volume volume = new Volume();
        volume.setName(source.getMetadata().getName());
        volume.setSecret(new SecretVolumeSource(420, null, false, source.getMetadata().getName()));

        return volume;
    }

    public static Volume volume(ConfigMap source) {
        Volume volume = new Volume();
        volume.setName(source.getMetadata().getName());
        volume.setConfigMap(new ConfigMapVolumeSource(420, null, source.getMetadata().getName(), false));

        return volume;
    }

    public static VolumeMount mount(HasMetadata source, String path) {
        return mount(source.getMetadata().getName(), path);
    }

    public static VolumeMount mount(String name, String path) {
        VolumeMount mount = new VolumeMount();
        mount.setMountPath(path);
        mount.setName(name);

        return mount;
    }

    public static EnvVar env(String name, String format, Object... args) {
        EnvVar env = new EnvVar();
        env.setName(name);
        env.setValue(String.format(format, args));

        return env;
    }

    public static Probe probe() {
        Probe probe = new Probe();
        probe.setFailureThreshold(3);
        probe.setInitialDelaySeconds(60);
        probe.setPeriodSeconds(10);
        probe.setSuccessThreshold(1);
        probe.setTimeoutSeconds(5);
        probe.setHttpGet(new HTTPGetAction(null, null, "/", new IntOrString("rest-api"), "HTTP"));

        return probe;
    }

    public static String jaasConfig(ServiceAccountSpec sa) {

        return String.format(
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
            sa.getClientId(),
            new String(Base64.getDecoder().decode(sa.getClientSecret()), StandardCharsets.UTF_8));
    }

    public static Map<String, String> createSchemaHistoryConfig(ManagedConnector connector, ServiceAccountSpec sa) {
        return Map.of(
            "schema.history.internal.kafka.bootstrap.servers", connector.getSpec().getDeployment().getKafka().getUrl(),
            "schema.history.internal.kafka.topic", connector.getMetadata().getName() + "-schema-history",

            "schema.history.internal.producer.security.protocol", "SASL_SSL",
            "schema.history.internal.producer.sasl.mechanism", "PLAIN",
            "schema.history.internal.producer.sasl.jaas.config", jaasConfig(sa),

            "schema.history.internal.consumer.security.protocol", "SASL_SSL",
            "schema.history.internal.consumer.sasl.mechanism", "PLAIN",
            "schema.history.internal.consumer.sasl.jaas.config", jaasConfig(sa));
    }

    public static ResourceRequirements resources() {
        return new ResourceRequirementsBuilder()
            .addToRequests("cpu", new Quantity("10m"))
            .addToRequests("memory", new Quantity("256Mi"))
            .addToLimits("cpu", new Quantity("500m"))
            .addToLimits("memory", new Quantity("1Gi"))
            .build();
    }

    public static Optional<DeploymentCondition> get(List<DeploymentCondition> conditions, String type) {
        if (conditions == null) {
            return Optional.empty();
        }

        return conditions.stream().filter(c -> Objects.equals(c.getType(), type)).findFirst();
    }

    public static void computeKafkaConnectCondition(ManagedConnector connector, Deployment d, Pod p) {
        if (d == null || d.getStatus() == null) {
            Conditions.set(
                connector.getStatus().getConnectorStatus().getConditions(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("DeploymentNotReady")
                    .withMessage("DeploymentNotReady")
                    .build());

            return;
        }

        if (p == null || p.getStatus() == null) {
            Conditions.set(
                connector.getStatus().getConnectorStatus().getConditions(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("PodNotReady")
                    .withMessage("PodNotReady")
                    .build());

            return;
        }

        if (p.getStatus().getContainerStatuses() != null && p.getStatus().getContainerStatuses().size() == 1) {
            ContainerStatus cs = p.getStatus().getContainerStatuses().get(0);

            if (cs.getState().getTerminated() != null) {
                String reason = cs.getState().getTerminated().getReason();
                String message = cs.getState().getTerminated().getMessage();

                if (Strings.isNullOrEmpty(message)) {
                    message = reason;
                }

                Conditions.set(
                    connector.getStatus().getConnectorStatus().getConditions(),
                    new ConditionBuilder()
                        .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                        .withStatus("False")
                        .withReason("Error")
                        .withMessage(message)
                        .build());

                return;
            } else if (cs.getState().getWaiting() != null) {
                String reason = cs.getState().getWaiting().getReason();
                String message = cs.getState().getWaiting().getMessage();

                if (Strings.isNullOrEmpty(message)) {
                    message = reason;
                }

                Conditions.set(
                    connector.getStatus().getConnectorStatus().getConditions(),
                    new ConditionBuilder()
                        .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                        .withStatus("False")
                        .withReason("ContainerNotready")
                        .withMessage(message)
                        .build());

                return;
            }
        }

        if (d.getSpec().getReplicas() == null) {
            Conditions.set(
                connector.getStatus().getConnectorStatus().getConditions(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("NotEnoughReplicas")
                    .withMessage("NotEnoughReplicas")
                    .build());

            return;
        }

        if (!Objects.equals(d.getSpec().getReplicas(), d.getStatus().getReadyReplicas())) {
            Conditions.set(
                connector.getStatus().getConnectorStatus().getConditions(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("NotEnoughReplicas")
                    .withMessage("NotEnoughReplicas")
                    .build());

            return;
        }

        Conditions.set(
            connector.getStatus().getConnectorStatus().getConditions(),
            new ConditionBuilder()
                .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                .withStatus("True")
                .withReason("Ready")
                .withMessage("Ready")
                .build());
    }

    public static void computeKafkaConnectorCondition(ManagedConnector connector, KafkaConnectorDetail detail) {
        Condition condition = new Condition();
        condition.setType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY);
        condition.setStatus("False");

        switch (detail.connector.state) {

            case KafkaConnectorDetail.STATE_FAILED: {
                String message = "DebeziumException";
                if (!Strings.isNullOrEmpty(detail.connector.trace)) {
                    message = detail.connector.trace;
                }

                condition.setReason("Error");
                condition.setMessage(message);

                break;
            }

            case KafkaConnectorStatus.STATE_UNASSIGNED: {
                condition.setReason("Unassigned");
                condition.setMessage("The connector is not yet assigned or re-balancing is in progress");

                break;
            }

            case KafkaConnectorStatus.STATE_PAUSED: {
                condition.setReason("Paused");
                condition.setMessage("The connector is paused");

                break;
            }

            case KafkaConnectorStatus.STATE_RUNNING: {

                boolean failed = false;
                ArrayList<String> errors = new ArrayList<>();

                for (KafkaConnectorTask task : detail.tasks) {
                    if (KafkaConnectorDetail.STATE_FAILED.equals(task.state)) {
                        failed = true;

                        if (!Strings.isNullOrEmpty(task.trace)) {
                            errors.add(task.trace);
                        }
                    }

                    if (KafkaConnectorDetail.STATE_RUNNING.equals(task.state)) {
                        failed = false;
                        errors.clear();
                    }
                }

                if (failed) {
                    String message = "DebeziumException";
                    if (!errors.isEmpty()) {
                        message = String.join("; \n", errors);
                    }

                    condition.setReason("Error");
                    condition.setMessage(message);
                } else {
                    condition.setStatus("True");
                    condition.setReason("Ready");
                    condition.setMessage("Ready");
                }

                break;
            }

            default:
                condition.setReason("Unavailable");
                condition.setMessage("Unknown connector status");

                break;
        }

        Conditions.set(
            connector.getStatus().getConnectorStatus().getConditions(),
            condition);
    }

    public static void computeConnectorCondition(ManagedConnector connector) {
        Condition ready = new Condition();
        ready.setType("Ready");
        ready.setStatus("False");
        ready.setReason("ConnectorNotReady");
        ready.setMessage("ConnectorNotReady");

        connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_PROVISIONING);

        Optional<Condition> kc = Conditions.get(connector.getStatus().getConnectorStatus().getConditions(),
            DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY);

        if (kc.isPresent()) {
            if ("False".equals(kc.get().getStatus()) && "Error".equals(kc.get().getReason())) {
                connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_FAILED);

                ready.setReason("Error");
                ready.setMessage(kc.get().getMessage());
            } else if ("False".equals(kc.get().getStatus())) {
                connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_PROVISIONING);

                ready.setReason("ConnectorNotReady");
                ready.setMessage(kc.get().getMessage());
            } else {

                Optional<Condition> kctr = Conditions.get(connector.getStatus().getConnectorStatus().getConditions(),
                    DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY);

                if (kctr.isPresent()) {
                    if ("False".equals(kctr.get().getStatus()) && "Error".equals(kctr.get().getReason())) {
                        connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_FAILED);

                        ready.setReason("Error");
                        ready.setMessage(kctr.get().getMessage());
                    } else if ("False".equals(kctr.get().getStatus()) && "Paused".equals(kctr.get().getReason())) {
                        connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_STOPPED);

                        ready.setReason("ConnectorPaused");
                        ready.setMessage(kctr.get().getMessage());
                    } else if ("False".equals(kctr.get().getStatus())) {
                        connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_PROVISIONING);

                        ready.setReason("ConnectorNotReady");
                        ready.setMessage(kctr.get().getMessage());
                    } else if ("True".equals(kctr.get().getStatus())) {
                        connector.getStatus().getConnectorStatus().setPhase(ManagedConnector.STATE_READY);

                        ready.setReason(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY);
                        ready.setMessage(kctr.get().getMessage());
                    }
                }
            }
        }

        Conditions.set(
            connector.getStatus().getConnectorStatus().getConditions(),
            ready);
    }

    public static Secret secret(KubernetesClient client, ManagedConnector connector) {
        return client.secrets()
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(secretName(connector))
            .get();
    }

    public static ConfigMap configmap(KubernetesClient client, ManagedConnector connector) {
        return client.configMaps()
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(configMapName(connector))
            .get();
    }

    public static PersistentVolumeClaim pvc(KubernetesClient client, ManagedConnector connector) {
        return client.persistentVolumeClaims()
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .get();
    }

    public static Service svc(KubernetesClient client, ManagedConnector connector) {
        return client.services()
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .get();
    }

    public static void connectorSpecToMap(ObjectNode connectorSpec, Map<String, String> config) {
        if (connectorSpec != null) {
            var cit = connectorSpec.fields();
            while (cit.hasNext()) {
                final var property = cit.next();

                if ("data_shape".equals(property.getKey())) {
                    continue;
                }
                if ("processors".equals(property.getKey())) {
                    continue;
                }
                if ("error_handler".equals(property.getKey())) {
                    continue;
                }

                if (!property.getValue().isObject()) {
                    config.put(
                        property.getKey(),
                        property.getValue().asText());
                } else {
                    JsonNode kind = property.getValue().requiredAt("/kind");
                    JsonNode value = property.getValue().requiredAt("/value");

                    if (!"base64".equals(kind.textValue())) {
                        throw new RuntimeException(
                            "Unsupported field kind " + kind + " (key=" + property.getKey() + ")");
                    }

                    config.putIfAbsent(
                        property.getKey(),
                        new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8));
                }
            }
        }
    }

    public static Map<String, String> createConnectorConfig(
        ManagedConnector connector,
        Map<String, String> configuration,
        DebeziumShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, DebeziumDataShape> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        Map<String, String> config = new TreeMap<>();

        // add external configuration
        if (configuration != null) {
            config.putAll(configuration);
        }

        connectorSpecToMap(connectorConfiguration.getConnectorSpec(), config);

        // handle connector config defaults
        // TODO: this could go into the connector metadata so in case a connector requires to a specific config
        //       for a specific version, then it is not required to release a new operator
        switch (shardMetadata.getConnectorClass()) {
            case CLASS_NAME_POSTGRES_CONNECTOR:
                config.putIfAbsent(CONFIG_OPTION_POSTGRES_PLUGIN_NAME, PLUGIN_NAME_PGOUTPUT);
                break;
            case CLASS_NAME_SQLSERVER_CONNECTOR:
                config.putIfAbsent(CONFIG_OPTION_SQLSERVER_DATABASE_ENCRYPT, "true");
                config.putIfAbsent(CONFIG_OPTION_SQLSERVER_DATABASE_TRUST_CERTIFICATE, "true");
                // TODO: this may be a configuration option int the connector shard metadata,
                //       i.e. requires.schema.history = true|false
                config.putAll(DebeziumOperandSupport.createSchemaHistoryConfig(connector, serviceAccountSpec));
                break;
            case CLASS_NAME_MYSQL_CONNECTOR:
                config.putAll(DebeziumOperandSupport.createSchemaHistoryConfig(connector, serviceAccountSpec));
                break;
            default:
                break;
        }

        return config;
    }

    public static String configMapName(ManagedConnector connector) {
        return connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX + "-resources";
    }

    public static String secretName(ManagedConnector connector) {
        return connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX + "-params";
    }
}
