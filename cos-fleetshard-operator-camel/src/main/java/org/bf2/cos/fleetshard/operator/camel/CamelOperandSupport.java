package org.bf2.cos.fleetshard.operator.camel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.EndpointKamelet;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_LOG_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_SINK_CHANNEL_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_STOP_URI;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_TYPE_DLQ;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_TYPE_LOG;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_TYPE_STOP;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_ID_PLACEHOLDER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_ID_PROPERTY;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_SECRET_PLACEHOLDER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_SECRET_PROPERTY;
import static org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint.kamelet;
import static org.bf2.cos.fleetshard.support.json.JacksonUtil.iterator;

public final class CamelOperandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelOperandSupport.class);

    private CamelOperandSupport() {
    }

    public static Optional<String> secretPropertyValue(JsonNode node) {
        if (node.isObject()) {
            JsonNode kind = node.requiredAt("/kind");
            JsonNode value = node.requiredAt("/value");

            if (!"base64".equals(kind.textValue())) {
                throw new IllegalArgumentException("Unsupported kind: " + kind);
            }

            return Optional.of(
                new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8));
        }

        return Optional.empty();
    }

    public static void configureKameletProperties(Map<String, Object> props, ObjectNode node, EndpointKamelet kamelet) {
        if (node == null) {
            return;
        }
        if (kamelet == null) {
            return;
        }

        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();
            final String key = property.getKey();

            if (key.startsWith(kamelet.getPrefix() + "_")) {
                props.put(
                    asPropertyKey(key, kamelet.getPrefix()),
                    asKameletPropertyValue(key, property.getValue()));
            }
        }
    }

    public static String asKameletPropertyValue(String key, JsonNode node) {
        if (node.isObject()) {
            JsonNode kind = node.requiredAt("/kind");
            if (!"base64".equals(kind.textValue())) {
                throw new IllegalArgumentException("Unsupported kind: " + kind);
            }

            return "{{" + key + "}}";
        } else {
            return node.asText();
        }
    }

    public static String asPropertyKey(String key, String prefix) {
        String answer = key.substring(prefix.length() + 1);
        return asPropertyKey(answer);
    }

    public static String asPropertyKey(String key) {
        if (!key.contains("_")) {
            return key;
        }
        return CaseUtils.toCamelCase(key, false, '_');
    }

    public static void configureEndpoint(Map<String, String> props, ObjectNode node) {
        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();

            secretPropertyValue(property.getValue()).ifPresent(
                val -> props.put(property.getKey(), val));
        }
    }

    public static KameletEndpoint configureStep(
        String templateId,
        ObjectNode node) {

        var step = kamelet(templateId);

        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();

            if (templateId == null) {
                throw new IllegalArgumentException("Unknown processor: " + property.getKey());
            }

            String propertyName = asPropertyKey(property.getKey());

            switch (property.getValue().getNodeType()) {
                case BOOLEAN:
                    step.getProperties().put(
                        propertyName,
                        property.getValue().booleanValue());
                    break;
                case NUMBER:
                    step.getProperties().put(
                        propertyName,
                        property.getValue().numberValue());
                    break;
                default:
                    step.getProperties().put(
                        propertyName,
                        property.getValue().asText());
                    break;

            }
        }

        return step;
    }

    public static List<KameletEndpoint> createSteps(
        ManagedConnector connector,
        ConnectorConfiguration<ObjectNode, ObjectNode> connectorConfiguration,
        CamelShardMetadata shardMetadata,
        KameletEndpoint kafkaEndpoint) {

        String consumes = Optional.ofNullable(connectorConfiguration.getDataShapeSpec())
            .map(spec -> spec.at("/consumes/format"))
            .filter(node -> !node.isMissingNode())
            .map(JsonNode::asText)
            .orElse(shardMetadata.getConsumes());
        String produces = Optional.ofNullable(connectorConfiguration.getDataShapeSpec())
            .map(spec -> spec.at("/produces/format"))
            .filter(node -> !node.isMissingNode())
            .map(JsonNode::asText)
            .orElse(shardMetadata.getProduces());

        final ArrayNode steps = connectorConfiguration.getProcessorsSpec();
        final List<KameletEndpoint> stepDefinitions = new ArrayList<>(steps.size() + 2);

        if (consumes != null) {
            switch (consumes) {
                case "application/json":
                    stepDefinitions.add(kamelet("cos-decoder-json-action", properties -> {
                        if (shardMetadata.getConsumesClass() != null) {
                            properties.put("contentClass", shardMetadata.getConsumesClass());
                        }
                    }));
                    break;
                case "avro/binary":
                    stepDefinitions.add(kamelet("cos-decoder-avro-action", properties -> {
                        if (shardMetadata.getConsumesClass() != null) {
                            properties.put("contentClass", shardMetadata.getConsumesClass());
                        }
                    }));
                    break;
                case "application/x-java-object":
                    stepDefinitions.add(kamelet("cos-decoder-pojo-action", properties -> {
                        if (shardMetadata.getConsumesClass() != null) {
                            properties.put("mimeType", produces);
                        }
                    }));
                    break;
                case "text/plain":
                case "application/octet-stream":
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported value format " + consumes);
            }
        }

        for (JsonNode step : steps) {
            var element = step.fields().next();

            String templateId = shardMetadata.getKamelets().getProcessors().get(element.getKey());
            if (templateId == null) {
                throw new IllegalArgumentException("Unknown processor: " + element.getKey());
            }

            stepDefinitions.add(
                configureStep(templateId, (ObjectNode) element.getValue()));
        }

        if (produces != null) {
            switch (produces) {
                case "application/json":
                    stepDefinitions.add(kamelet("cos-encoder-json-action", properties -> {
                        if (shardMetadata.getProducesClass() != null) {
                            properties.put("contentClass", shardMetadata.getProducesClass());
                        }
                    }));
                    break;
                case "avro/binary":
                    stepDefinitions.add(kamelet("cos-encoder-avro-action", properties -> {
                        if (shardMetadata.getProducesClass() != null) {
                            properties.put("contentClass", shardMetadata.getProducesClass());
                        }
                    }));
                    break;
                case "text/plain":
                    stepDefinitions.add(kamelet("cos-encoder-string-action"));
                    break;
                case "application/octet-stream":
                    stepDefinitions.add(kamelet("cos-encoder-bytearray-action"));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported value format " + produces);
            }
        }

        // If it is a sink, then it consumes from kafka
        if (isSink(shardMetadata)) {
            String valueDeserializer = "org.bf2.cos.connector.camel.serdes.bytes.ByteArrayDeserializer";

            if ("application/json".equals(consumes) && hasSchemaRegistry(connector)) {
                valueDeserializer = "org.bf2.cos.connector.camel.serdes.json.JsonDeserializer";
            } else if ("avro/binary".equals(produces) && hasSchemaRegistry(connector)) {
                valueDeserializer = "org.bf2.cos.connector.camel.serdes.avro.AvroDeserializer";
            }

            kafkaEndpoint.getProperties().put("valueDeserializer", valueDeserializer);
        }

        // If it is a source, then it produces to kafka
        if (isSource(shardMetadata)) {
            String valueSerializer = "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer";

            if ("application/json".equals(produces) && hasSchemaRegistry(connector)) {
                valueSerializer = "org.bf2.cos.connector.camel.serdes.json.JsonSerializer";
            } else if ("avro/binary".equals(produces) && hasSchemaRegistry(connector)) {
                valueSerializer = "org.bf2.cos.connector.camel.serdes.avro.AvroSerializer";
            }

            kafkaEndpoint.getProperties().put("valueSerializer", valueSerializer);
        }

        return stepDefinitions;
    }

    /**
     * Generates a properties map to be stored as a secret in kubernetes. Properties in this secret are to be used by the camel
     * implementation running the connector.
     */
    public static Map<String, String> createSecretsData(
        ManagedConnector connector,
        ConnectorConfiguration<ObjectNode, ObjectNode> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec,
        CamelOperandConfiguration cfg) {

        Map<String, String> props = new TreeMap<>();

        ObjectNode connectorConfigurationSpec = connectorConfiguration.getConnectorSpec();
        if (connectorConfigurationSpec != null) {
            configureEndpoint(props, connectorConfigurationSpec);

            props.put(
                SA_CLIENT_ID_PROPERTY,
                serviceAccountSpec.getClientId());
            props.put(
                SA_CLIENT_SECRET_PROPERTY,
                new String(Base64.getDecoder().decode(serviceAccountSpec.getClientSecret()), StandardCharsets.UTF_8));
        }

        // always enable supervising route controller, so that camel pods are not killed in case of failure
        // this way we can check it's health and report failing connectors
        props.put("camel.main.route-controller-supervise-enabled", "true");

        // when starting a route (and restarts) fails all attempts then we can control whether the route
        // should influence the health-check and report the route as either UNKNOWN or DOWN. Setting this
        // option to true will report it as DOWN otherwise its UNKNOWN
        props.put("camel.main.route-controller-unhealthy-on-exhausted", "true");

        // always enable camel health checks so we can monitor the connector
        props.put("camel.main.load-health-checks", "true");
        props.put("camel.health.routesEnabled", "true");
        props.put("camel.health.consumersEnabled", "true");
        props.put("camel.health.registryEnabled", "true");

        if (cfg.routeController() != null) {
            props.put("camel.main.route-controller-backoff-delay", cfg.routeController().backoffDelay());
            props.put("camel.main.route-controller-initial-delay", cfg.routeController().initialDelay());
            props.put("camel.main.route-controller-backoff-multiplier", cfg.routeController().backoffMultiplier());
            props.put("camel.main.route-controller-backoff-max-attempts", cfg.routeController().backoffMaxAttempts());
        }

        if (cfg.exchangePooling() != null) {
            props.put(
                "camel.main.exchange-factory",
                cfg.exchangePooling().exchangeFactory());
            props.put(
                "camel.main.exchange-factory-capacity",
                cfg.exchangePooling().exchangeFactoryCapacity());
            props.put(
                "camel.main.exchange-factory-statistics-enabled",
                cfg.exchangePooling().exchangeFactoryStatisticsEnabled());
        }

        return props;
    }

    /**
     * Generates the integration node that holds camel configurations.
     */
    public static ObjectNode createIntegrationSpec(
        String secretName,
        CamelOperandConfiguration cfg,
        Map<String, String> envVars) {

        ObjectNode integration = Serialization.jsonMapper().createObjectNode();
        integration.put("profile", CamelConstants.CAMEL_K_PROFILE_OPENSHIFT);
        ArrayNode configuration = integration.withArray("configuration");

        configuration.addObject()
            .put("type", "secret")
            .put("value", secretName);

        envVars.forEach((k, v) -> {
            configuration.addObject()
                .put("type", "env")
                .put("value", k + "=" + v);
        });

        return integration;
    }

    public static Optional<KameletBinding> lookupBinding(KubernetesClient client, ManagedConnector connector) {
        return Optional.ofNullable(
            client.resources(KameletBinding.class)
                .inNamespace(connector.getMetadata().getNamespace())
                .withName(connector.getMetadata().getName())
                .get());
    }

    public static void computeStatus(ConnectorStatusSpec statusSpec, KameletBindingStatus kameletBindingStatus) {
        if (kameletBindingStatus.phase != null) {
            switch (kameletBindingStatus.phase.toLowerCase(Locale.US)) {
                case KameletBindingStatus.PHASE_READY:
                    statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                    if (kameletBindingStatus.conditions != null) {
                        boolean readyCondition = kameletBindingStatus.conditions.stream()
                            .anyMatch(cond -> "Ready".equals(cond.getType())
                                && "True".equals(cond.getStatus()));
                        if (readyCondition) {
                            statusSpec.setPhase(ManagedConnector.STATE_READY);
                        }
                    }
                    break;
                case KameletBindingStatus.PHASE_ERROR:
                    statusSpec.setPhase(ManagedConnector.STATE_FAILED);
                    break;
                default:
                    statusSpec.setPhase(ManagedConnector.STATE_PROVISIONING);
                    break;
            }
        }

        if (kameletBindingStatus.conditions != null) {
            for (Condition condition : kameletBindingStatus.conditions) {
                // This cleanup is needed as the KameletBinding condition has a field, lastUpdateTime, that
                // does not map to any field in the Condition class provided by the Fabric8 Kubernetes Client.
                //
                // Such field is then kept as an additional property and causes additional reconciliation loops
                // as the Fabric8 Kubernetes Client generate a wrong JSON patch.
                condition.setAdditionalProperties(null);
            }

            statusSpec.setConditions(kameletBindingStatus.conditions);
        }
    }

    public static ObjectNode createErrorHandler(
        CamelShardMetadata shardMetadata,
        ManagedConnector connector,
        ObjectNode errorHandlerSpec) {

        if (errorHandlerSpec != null && !errorHandlerSpec.isEmpty()) {
            // Assume only one is populated because of prior validation
            String errorHandlerType = errorHandlerSpec.fieldNames().next();
            return createErrorHandler(errorHandlerType, connector, errorHandlerSpec);
        } else if (StringUtils.isNotBlank(shardMetadata.getErrorHandlerStrategy())) {
            return createErrorHandler(shardMetadata.getErrorHandlerStrategy(), connector, null);
        } else {
            LOGGER.warn("No error handler specified and no default found for connector: {}", connector.getSpec());
        }
        return null;
    }

    public static ObjectNode createErrorHandler(
        String errorHandlerType,
        ManagedConnector connector,
        ObjectNode errorHandlerSpec) {

        switch (errorHandlerType.toLowerCase(Locale.ROOT)) {
            case ERROR_HANDLER_TYPE_LOG:
                return createLogErrorHandler();
            case ERROR_HANDLER_TYPE_STOP:
                return createStopErrorHandler();
            case ERROR_HANDLER_TYPE_DLQ:
                return createDeadLetterQueueErrorHandler(connector, errorHandlerSpec);
            default: {
                throw new RuntimeException("Invalid error handling specification: " + errorHandlerSpec.asText());
            }
        }
    }

    public static ObjectNode createLogErrorHandler() {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        errorHandler.putObject(ERROR_HANDLER_LOG_TYPE);
        return errorHandler;
    }

    public static ObjectNode createStopErrorHandler() {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        var dlq = errorHandler.putObject(ERROR_HANDLER_SINK_CHANNEL_TYPE);
        var endpoint = dlq.putObject("endpoint");
        endpoint.put("uri", ERROR_HANDLER_STOP_URI);
        return errorHandler;
    }

    public static ObjectNode createDeadLetterQueueErrorHandler(ManagedConnector connector, ObjectNode spec) {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        var dlq = errorHandler.putObject(ERROR_HANDLER_SINK_CHANNEL_TYPE);
        var endpoint = dlq.putObject("endpoint");

        endpoint.with("ref")
            .put("kind", Kamelet.RESOURCE_KIND)
            .put("apiVersion", Kamelet.RESOURCE_API_VERSION)
            .put("name", ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET);
        endpoint.with("properties")
            .put("user", SA_CLIENT_ID_PLACEHOLDER)
            .put("password", SA_CLIENT_SECRET_PLACEHOLDER)
            .put("bootstrapServers", connector.getSpec().getDeployment().getKafka().getUrl())
            .set("topic", spec.requiredAt("/dead_letter_queue/topic"));

        dlq.putObject("parameters")
            .put("useOriginalMessage", "true");

        return errorHandler;
    }

    public static boolean isSource(CamelShardMetadata shardMetadata) {
        return CONNECTOR_TYPE_SOURCE.equals(shardMetadata.getConnectorType());
    }

    public static boolean isSink(CamelShardMetadata shardMetadata) {
        return CONNECTOR_TYPE_SINK.equals(shardMetadata.getConnectorType());
    }

    public static boolean hasSchemaRegistry(ManagedConnector connector) {
        if (connector.getSpec().getDeployment().getSchemaRegistry() == null) {
            return false;
        }

        return StringUtils.isNotEmpty(connector.getSpec().getDeployment().getSchemaRegistry().getUrl());
    }
}
