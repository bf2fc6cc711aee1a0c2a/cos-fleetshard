package org.bf2.cos.fleetshard.operator.camel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static java.lang.String.format;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_LOG_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_NONE_TYPE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_STOP_URI;
import static org.bf2.cos.fleetshard.support.json.JacksonUtil.iterator;

public final class CamelOperandSupport {
    private CamelOperandSupport() {
    }

    public static boolean isKameletBinding(ResourceRef ref) {
        return Objects.equals(KameletBinding.RESOURCE_API_VERSION, ref.getApiVersion())
            && Objects.equals(KameletBinding.RESOURCE_KIND, ref.getKind());
    }

    public static void configureEndpoint(Map<String, String> props, ObjectNode node, String templateId) {
        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();
            final JsonNode pval = property.getValue();
            final String pkey = format(
                "camel.kamelet.%s.%s",
                templateId,
                property.getKey());

            if (pval.isObject()) {
                JsonNode kind = pval.requiredAt("/kind");
                JsonNode value = pval.requiredAt("/value");

                if (!"base64".equals(kind.textValue())) {
                    throw new RuntimeException(
                        "Unsupported field kind " + kind + " (key=" + pkey + ")");
                }

                props.put(pkey, new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8));
            } else {
                props.put(pkey, pval.asText());
            }
        }
    }

    public static void configureStep(Map<String, String> props, ObjectNode node, int index, String templateId) {
        for (Iterator<Map.Entry<String, JsonNode>> cit = iterator(node); cit.hasNext();) {
            final var property = cit.next();
            final JsonNode pval = property.getValue();
            final String pkey = format(
                "camel.kamelet.%s.%s.%s",
                templateId,
                stepName(index, templateId),
                property.getKey());

            if (pval.isObject()) {
                JsonNode kind = pval.requiredAt("/kind");
                JsonNode value = pval.requiredAt("/value");

                if (!"base64".equals(kind.textValue())) {
                    throw new RuntimeException(
                        "Unsupported field kind " + kind + " (key=" + pkey + ")");
                }

                props.put(pkey, new String(Base64.getDecoder().decode(value.asText()), StandardCharsets.UTF_8));
            } else {
                props.put(pkey, pval.asText());
            }
        }
    }

    public static String stepName(int index, String templateId) {
        return templateId + "-" + index;
    }

    public static List<Step> createSteps(JsonNode connectorSpec, CamelShardMetadata shardMetadata) {
        final List<Step> stepDefinitions = new ArrayList<>();

        JsonNode steps = connectorSpec.at("/steps");
        for (int i = 0; i < steps.size(); i++) {
            var element = steps.get(i).fields().next();
            var templateId = shardMetadata.getKamelets().get(element.getKey());

            stepDefinitions.add(new Step(
                templateId,
                stepName(i, templateId)));
        }

        return stepDefinitions;
    }

    public static Map<String, String> createSecretsData(
        CamelShardMetadata shardMetadata,
        ObjectNode connectorSpec,
        KafkaSpec kafkaSpec) {

        final String connectorKameletId = shardMetadata.getKamelets().get("connector");
        final String kafkaKameletId = shardMetadata.getKamelets().get("kafka");

        Map<String, String> props = new HashMap<>();
        if (connectorSpec != null) {
            configureEndpoint(
                props,
                (ObjectNode) connectorSpec.get("connector"),
                connectorKameletId);
            configureEndpoint(
                props,
                (ObjectNode) connectorSpec.get("kafka"),
                kafkaKameletId);

            props.put(
                format("camel.kamelet.%s.user", kafkaKameletId),
                kafkaSpec.getClientId());
            props.put(
                format("camel.kamelet.%s.password", kafkaKameletId),
                new String(Base64.getDecoder().decode(kafkaSpec.getClientSecret()), StandardCharsets.UTF_8));
            props.put(
                format("camel.kamelet.%s.bootstrapServers", kafkaKameletId),
                kafkaSpec.getBootstrapServers());

            var steps = connectorSpec.at("/steps");
            for (int i = 0; i < steps.size(); i++) {
                var element = steps.get(i).fields().next();
                var templateId = shardMetadata.getKamelets().get(element.getKey());

                configureStep(
                    props,
                    (ObjectNode) element.getValue(),
                    i,
                    templateId);
            }

            var errorHandler = (ObjectNode) connectorSpec.get("error_handling");
            if (errorHandler != null) {
                var dlq = (ObjectNode) errorHandler.get("dead_letter_queue");
                if (dlq != null) {
                    String errorKamelet = ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET;
                    String errorKameletId = ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID;
                    JsonNode dlTopic = dlq.get("topic");
                    if (dlTopic == null) {
                        throw new RuntimeException("Missing topic property in dead_letter_queue error handler");
                    }
                    props.put(
                        format("camel.kamelet.%s.%s.user", errorKamelet, errorKameletId),
                        kafkaSpec.getClientId());
                    props.put(
                        format("camel.kamelet.%s.%s.password", errorKamelet, errorKameletId),
                        new String(Base64.getDecoder().decode(kafkaSpec.getClientSecret()), StandardCharsets.UTF_8));
                    props.put(
                        format("camel.kamelet.%s.%s.bootstrapServers", errorKamelet, errorKameletId),
                        kafkaSpec.getBootstrapServers());
                    props.put(
                        format("camel.kamelet.%s.%s.topic", errorKamelet, errorKameletId),
                        dlTopic.asText());
                }
            }
        }

        return props;
    }

    public static ObjectNode createIntegrationSpec(
        String secretName,
        CamelOperandConfiguration cfg,
        Map<String, String> envVars) {

        ObjectNode integration = Serialization.jsonMapper().createObjectNode();
        ArrayNode configuration = integration.withArray("configuration");

        configuration.addObject()
            .put("type", "secret")
            .put("value", secretName);

        envVars.forEach((k, v) -> {
            configuration.addObject()
                .put("type", "env")
                .put("value", k + "=" + v);
        });

        if (cfg.configurations() != null) {
            for (var c : cfg.configurations()) {
                configuration.addObject()
                    .put("type", c.type())
                    .put("value", c.value());
            }
        }

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
            statusSpec.setConditions(kameletBindingStatus.conditions);
        }
    }

    public static ObjectNode createErrorHandler(ObjectNode connectorSpec) {
        if (connectorSpec != null) {
            var errorHandling = (JsonNode) connectorSpec.get("error_handling");
            if (errorHandling != null) {
                // Assume only one is populated because of prior validation
                if (errorHandling.get("log") != null) {
                    return createLogErrorHandler();
                } else if (errorHandling.get("stop") != null) {
                    return createStopErrorHandler();
                } else if (errorHandling.get("ignore") != null) {
                    return createIgnoreErrorHandler();
                } else if (errorHandling.get("dead_letter_queue") != null) {
                    return createDeadLetterQueueErrorHandler();
                } else {
                    throw new RuntimeException("Invalid error handling specification: " + errorHandling.asText());
                }
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
        var dlq = errorHandler.putObject(ERROR_HANDLER_DEAD_LETTER_CHANNEL_TYPE);
        var endpoint = dlq.putObject("endpoint");
        endpoint.put("uri", ERROR_HANDLER_STOP_URI);
        return errorHandler;
    }

    public static ObjectNode createIgnoreErrorHandler() {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        errorHandler.putObject(ERROR_HANDLER_NONE_TYPE);
        return errorHandler;
    }

    public static ObjectNode createDeadLetterQueueErrorHandler() {
        var errorHandler = Serialization.jsonMapper().createObjectNode();
        var dlq = errorHandler.putObject(ERROR_HANDLER_DEAD_LETTER_CHANNEL_TYPE);
        var endpoint = dlq.putObject("endpoint");
        endpoint.put("uri",
            format("kamelet://%s/%s", ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET, ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID));
        return errorHandler;
    }

    public static class Step {
        final String templateId;
        final String id;

        public Step(String templateId, String id) {
            this.templateId = templateId;
            this.id = id;
        }
    }

}
