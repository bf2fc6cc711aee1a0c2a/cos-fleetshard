package org.bf2.cos.fleetshard.operator.camel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelets;
import org.bf2.cos.fleetshard.operator.camel.model.ProcessorKamelet;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_LOG_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_SINK_CHANNEL_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_STOP_URI;
import static org.bf2.cos.fleetshard.support.json.JacksonUtil.iterator;

public final class CamelOperandSupport {

    private CamelOperandSupport() {
    }

    public static String kameletProperty(String templateId, String property) {
        return String.format("camel.kamelet.%s.%s", templateId, property);
    }

    public static String kameletProperty(String templateId, String instanceName, String property) {
        return String.format("camel.kamelet.%s.%s.%s", templateId, instanceName, property);
    }

    public static String asPropertyValue(JsonNode node) {
        if (node.isObject()) {
            JsonNode kind = node.requiredAt("/kind");
            JsonNode value = node.requiredAt("/value");

            if (!"base64".equals(kind.textValue())) {
                throw new IllegalArgumentException("Unsupported kind: " + kind);
            }

            return new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8);
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

    public static String stepName(int index, String templateId) {
        return templateId + "-" + index;
    }

    public static void configureEndpoint(Map<String, String> props, ObjectNode node, Kamelets mapping) {

        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();
            final String key = property.getKey();

            final String templateId;
            final String prefix;
            if (key.startsWith(mapping.getAdapter().getPrefix() + "_")) {
                templateId = mapping.getAdapter().getName();
                prefix = mapping.getAdapter().getPrefix();
            } else if (key.startsWith(mapping.getKafka().getPrefix() + "_")) {
                templateId = mapping.getKafka().getName();
                prefix = mapping.getKafka().getPrefix();
            } else {
                throw new IllegalArgumentException("Unknown property: " + key);
            }

            String propertyName = asPropertyKey(key, prefix);

            props.put(
                kameletProperty(templateId, propertyName),
                asPropertyValue(property.getValue()));
        }
    }

    public static void configureStep(
        Map<String, String> props,
        ObjectNode node,
        int index,
        String templateId) {

        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();

            if (templateId == null) {
                throw new IllegalArgumentException("Unknown processor: " + property.getKey());
            }

            String propertyName = asPropertyKey(property.getKey());

            props.put(
                kameletProperty(templateId, stepName(index, templateId), propertyName),
                asPropertyValue(property.getValue()));
        }
    }

    public static List<ProcessorKamelet> createSteps(
        ManagedConnector connector,
        ConnectorConfiguration<ObjectNode, ObjectNode> connectorConfiguration,
        CamelShardMetadata shardMetadata,
        Map<String, String> props) {

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
        final List<ProcessorKamelet> stepDefinitions = new ArrayList<>(steps.size() + 2);

        int i = 0;

        if (consumes != null) {
            switch (consumes) {
                case "application/json": {
                    String stepName = stepName(i, "cos-decoder-json-action");
                    stepDefinitions.add(new ProcessorKamelet("cos-decoder-json-action", stepName));
                    if (shardMetadata.getConsumesClass() != null) {
                        props.put(
                            kameletProperty("cos-decoder-json-action", stepName, "contentClass"),
                            shardMetadata.getConsumesClass());
                    }
                    i++;
                }
                    break;
                case "avro/binary": {
                    String stepName = stepName(i, "cos-decoder-avro-action");
                    stepDefinitions.add(new ProcessorKamelet("cos-decoder-avro-action", stepName));
                    if (shardMetadata.getConsumesClass() != null) {
                        props.put(
                            kameletProperty("cos-decoder-avro-action", stepName, "contentClass"),
                            shardMetadata.getConsumesClass());
                    }
                    i++;
                }
                    break;
                case "application/x-java-object": {
                    String stepName = stepName(i, "cos-decoder-pojo-action");
                    stepDefinitions.add(new ProcessorKamelet("cos-decoder-pojo-action", stepName));
                    if (produces != null) {
                        props.put(
                            kameletProperty("cos-decoder-pojo-action", stepName, "mimeType"),
                            produces);
                    }
                    i++;
                }
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

            stepDefinitions.add(new ProcessorKamelet(templateId, stepName(i, templateId)));

            configureStep(
                props,
                (ObjectNode) element.getValue(),
                i,
                shardMetadata.getKamelets().getProcessors().get(element.getKey()));

            i++;
        }

        if (produces != null) {
            switch (produces) {
                case "application/json": {
                    String stepName = stepName(i, "cos-encoder-json-action");
                    stepDefinitions.add(new ProcessorKamelet("cos-encoder-json-action", stepName));
                    if (shardMetadata.getProducesClass() != null) {
                        props.put(
                            kameletProperty("cos-encoder-json-action", stepName, "contentClass"),
                            shardMetadata.getProducesClass());
                    }
                }
                    break;
                case "avro/binary": {
                    String stepName = stepName(i, "cos-encoder-avro-action");
                    stepDefinitions.add(new ProcessorKamelet("cos-encoder-avro-action", stepName));
                    if (shardMetadata.getProducesClass() != null) {
                        props.put(
                            kameletProperty("cos-encoder-avro-action", stepName, "contentClass"),
                            shardMetadata.getProducesClass());
                    }
                }
                    break;
                case "text/plain": {
                    stepDefinitions.add(new ProcessorKamelet(
                        "cos-encoder-string-action",
                        stepName(i, "cos-encoder-string-action")));
                }
                    break;
                case "application/octet-stream": {
                    stepDefinitions.add(new ProcessorKamelet(
                        "cos-encoder-bytearray-action",
                        stepName(i, "cos-encoder-bytearray-action")));
                }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported value format " + produces);
            }
        }

        //
        // TODO: refactor
        //
        // The code below is a POC (well, almost all this class looks like a POC)
        //

        // If it is a sink, then it consumes from kafka
        if (isSink(shardMetadata)) {
            props.put(
                String.format("camel.kamelet.%s.valueDeserializer", shardMetadata.getKamelets().getKafka().getName()),
                "org.bf2.cos.connector.camel.serdes.bytes.ByteArrayDeserializer");

            if ("application/json".equals(consumes) && hasSchemaRegistry(connector)) {
                props.put(
                    String.format("camel.kamelet.%s.valueDeserializer", shardMetadata.getKamelets().getKafka().getName()),
                    "org.bf2.cos.connector.camel.serdes.json.JsonDeserializer");
            }
            if ("avro/binary".equals(produces) && hasSchemaRegistry(connector)) {
                props.put(
                    String.format("camel.kamelet.%s.valueDeserializer", shardMetadata.getKamelets().getKafka().getName()),
                    "org.bf2.cos.connector.camel.serdes.avro.AvroDeserializer");
            }
        }

        // If it is a source, then it produces to kafka
        if (isSource(shardMetadata)) {
            props.put(
                String.format("camel.kamelet.%s.valueSerializer", shardMetadata.getKamelets().getKafka().getName()),
                "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer");

            if ("application/json".equals(produces) && hasSchemaRegistry(connector)) {
                props.put(
                    String.format("camel.kamelet.%s.valueSerializer", shardMetadata.getKamelets().getKafka().getName()),
                    "org.bf2.cos.connector.camel.serdes.json.JsonSerializer");
            }
            if ("avro/binary".equals(produces) && hasSchemaRegistry(connector)) {
                props.put(
                    String.format("camel.kamelet.%s.valueSerializer", shardMetadata.getKamelets().getKafka().getName()),
                    "org.bf2.cos.connector.camel.serdes.avro.AvroSerializer");
            }
        }

        return stepDefinitions;
    }

    /**
     * Generates a properties map to be stored as a secret in kubernetes. Properties in this secret are to be used by the camel
     * implementation running the connector.
     */
    public static Map<String, String> createSecretsData(
        ManagedConnector connector,
        CamelShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, ObjectNode> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec,
        CamelOperandConfiguration cfg,
        Map<String, String> props) {

        ObjectNode connectorConfigurationSpec = connectorConfiguration.getConnectorSpec();
        if (connectorConfigurationSpec != null) {
            configureEndpoint(props, connectorConfigurationSpec, shardMetadata.getKamelets());

            props.put(
                String.format("camel.kamelet.%s.user", shardMetadata.getKamelets().getKafka().getName()),
                serviceAccountSpec.getClientId());
            props.put(
                String.format("camel.kamelet.%s.password", shardMetadata.getKamelets().getKafka().getName()),
                new String(Base64.getDecoder().decode(serviceAccountSpec.getClientSecret()), StandardCharsets.UTF_8));
            props.put(
                String.format("camel.kamelet.%s.bootstrapServers", shardMetadata.getKamelets().getKafka().getName()),
                connector.getSpec().getDeployment().getKafka().getUrl());

            if (CONNECTOR_TYPE_SINK.equals(shardMetadata.getConnectorType())) {
                props.put(
                    String.format("camel.kamelet.%s.consumerGroup", shardMetadata.getKamelets().getKafka().getName()),
                    connector.getSpec().getDeploymentId());
            }

            if (hasSchemaRegistry(connector)) {
                props.put(
                    String.format("camel.kamelet.%s.registryUrl", shardMetadata.getKamelets().getKafka().getName()),
                    connector.getSpec().getDeployment().getSchemaRegistry().getUrl());
            }

            ObjectNode errorHandlerSpec = connectorConfiguration.getErrorHandlerSpec();
            if (null != errorHandlerSpec) {
                var dlq = errorHandlerSpec.at("/dead_letter_queue");
                if (!dlq.isMissingNode()) {
                    String errorKamelet = ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET;
                    String errorKameletId = ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID;
                    JsonNode dlTopic = dlq.get("topic");
                    if (dlTopic == null) {
                        throw new RuntimeException("Missing topic property in dead_letter_queue error handler");
                    }
                    props.put(
                        String.format("camel.kamelet.%s.%s.user", errorKamelet, errorKameletId),
                        serviceAccountSpec.getClientId());
                    props.put(
                        String.format("camel.kamelet.%s.%s.password", errorKamelet, errorKameletId),
                        new String(Base64.getDecoder().decode(serviceAccountSpec.getClientSecret()), StandardCharsets.UTF_8));
                    props.put(
                        String.format("camel.kamelet.%s.%s.bootstrapServers", errorKamelet, errorKameletId),
                        connector.getSpec().getDeployment().getKafka().getUrl());
                    props.put(
                        String.format("camel.kamelet.%s.%s.topic", errorKamelet, errorKameletId),
                        dlTopic.asText());
                }
            }
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
        props.put("camel.health.contextEnabled", "true");
        props.put("camel.health.routesEnabled", "true");
        props.put("camel.health.consumersEnabled", "true");
        props.put("camel.health.registryEnabled", "true");
        props.put("camel.health.config[*].parent", "routes");
        props.put("camel.health.config[*].enabled", "true");

        if (cfg.routeController() != null) {
            props.put("camel.main.route-controller-backoff-delay", cfg.routeController().backoffDelay());
            props.put("camel.main.route-controller-initial-delay", cfg.routeController().initialDelay());
            props.put("camel.main.route-controller-backoff-multiplier", cfg.routeController().backoffMultiplier());
        }

        if (cfg.exchangePooling() != null) {
            props.put("camel.main.exchange-factory", cfg.exchangePooling().exchangeFactory());
            props.put("camel.main.exchange-factory-capacity", cfg.exchangePooling().exchangeFactoryCapacity());
            props.put("camel.main.exchange-factory-statistics-enabled",
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
                    statusSpec.setPhase(ManagedConnector.STATE_READY);
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

    public static ObjectNode createErrorHandler(ObjectNode errorHandlerSpec) {
        if (errorHandlerSpec != null) {
            // Assume only one is populated because of prior validation
            if (errorHandlerSpec.get("log") != null) {
                return createLogErrorHandler();
            } else if (errorHandlerSpec.get("stop") != null) {
                return createStopErrorHandler();
            } else if (errorHandlerSpec.get("dead_letter_queue") != null) {
                return createDeadLetterQueueErrorHandler();
            } else {
                throw new RuntimeException("Invalid error handling specification: " + errorHandlerSpec.asText());
            }
        }
        return null;
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

    public static ObjectNode createDeadLetterQueueErrorHandler() {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        var dlq = errorHandler.putObject(ERROR_HANDLER_SINK_CHANNEL_TYPE);
        var endpoint = dlq.putObject("endpoint");

        endpoint.put(
            "uri",
            String.format(
                "kamelet://%s/%s",
                ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET,
                ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID));

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
