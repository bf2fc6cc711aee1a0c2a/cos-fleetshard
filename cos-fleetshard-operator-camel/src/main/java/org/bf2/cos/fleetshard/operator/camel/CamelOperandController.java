package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.camel.CamelOperandConfiguration.Health;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingSpecBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
import org.bf2.cos.fleetshard.operator.camel.model.ProcessorKamelet;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.APPLICATION_PROPERTIES;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_FAILURE_THRESHOLD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PERIOD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_SUCCESS_THRESHOLD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_TIMEOUT;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_FAILURE_THRESHOLD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PERIOD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_SUCCESS_THRESHOLD;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_TIMEOUT;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.computeStatus;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createErrorHandler;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createIntegrationSpec;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSecretsData;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSteps;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.lookupBinding;
import static org.bf2.cos.fleetshard.support.CollectionUtils.asBytesBase64;

@Singleton
public class CamelOperandController extends AbstractOperandController<CamelShardMetadata, ObjectNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelOperandController.class);

    private final CamelOperandConfiguration configuration;

    public CamelOperandController(KubernetesClient kubernetesClient, CamelOperandConfiguration configuration) {
        super(kubernetesClient, CamelShardMetadata.class, ObjectNode.class);

        this.configuration = configuration;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return List.of(KameletBinding.RESOURCE_DEFINITION);
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnector connector,
        CamelShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        final Map<String, String> properties = createSecretsData(
            connector,
            shardMetadata,
            connectorConfiguration,
            serviceAccountSpec,
            configuration,
            new TreeMap<>());
        final List<ProcessorKamelet> stepDefinitions = createSteps(
            connectorConfiguration,
            shardMetadata,
            properties);

        final String source;
        final String sink;

        switch (shardMetadata.getConnectorType()) {
            case CONNECTOR_TYPE_SOURCE:
                source = shardMetadata.getKamelets().getAdapter().getName();
                sink = shardMetadata.getKamelets().getKafka().getName();
                break;
            case CONNECTOR_TYPE_SINK:
                source = shardMetadata.getKamelets().getKafka().getName();
                sink = shardMetadata.getKamelets().getAdapter().getName();
                break;
            default:
                throw new IllegalArgumentException("Unknown connector type: " + shardMetadata.getConnectorType());
        }

        final Secret secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX)
                .build())
            .addToData(APPLICATION_PROPERTIES, asBytesBase64(properties))
            .build();

        final KameletBinding binding = new KameletBindingBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .build())
            .withSpec(new KameletBindingSpecBuilder()
                .withIntegration(createIntegrationSpec(
                    secret.getMetadata().getName(),
                    configuration,
                    Map.of(
                        "CONNECTOR_SECRET_NAME", secret.getMetadata().getName(),
                        "CONNECTOR_SECRET_CHECKSUM", Secrets.computeChecksum(secret),
                        "CONNECTOR_ID", connector.getSpec().getConnectorId(),
                        "CONNECTOR_DEPLOYMENT_ID", connector.getSpec().getDeploymentId())))
                .withSource(KameletEndpoint.kamelet(source, Map.of("id", connector.getSpec().getDeploymentId())))
                .withSink(KameletEndpoint.kamelet(sink, Map.of("id", connector.getSpec().getDeploymentId())))
                .withErrorHandler(createErrorHandler(connectorConfiguration.getErrorHandlerSpec()))
                .withSteps(
                    stepDefinitions.stream()
                        .map(s -> KameletEndpoint.kamelet(s.getTemplateId(), Map.of("id", s.getId())))
                        .collect(Collectors.toList()))
                .build())
            .build();

        Map<String, String> annotations = KubernetesResourceUtil.getOrCreateAnnotations(binding);
        if (shardMetadata.getAnnotations() != null) {
            annotations.putAll(shardMetadata.getAnnotations());
        }

        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE, shardMetadata.getConnectorImage());
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS, LABELS_TO_TRANSFER);

        // health check annotations
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED, "true");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED, "true");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED, "true");
        Health health = configuration.health();
        if (health != null) {
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_SUCCESS_THRESHOLD,
                health.readinessSuccessThreshold());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_FAILURE_THRESHOLD,
                health.readinessFailureThreshold());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PERIOD, health.readinessPeriodSeconds());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_TIMEOUT, health.readinessTimeoutSeconds());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_SUCCESS_THRESHOLD,
                health.livenessSuccessThreshold());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_FAILURE_THRESHOLD,
                health.livenessFailureThreshold());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PERIOD, health.livenessPeriodSeconds());
            annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_TIMEOUT, health.livenessTimeoutSeconds());
        }

        return List.of(secret, binding);
    }

    @Override
    public void status(ManagedConnector connector) {
        lookupBinding(getKubernetesClient(), connector)
            .ifPresent(klb -> computeStatus(connector.getStatus().getConnectorStatus(), klb.getStatus()));
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return delete(connector);
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        if (connector.getStatus().getConnectorStatus() == null) {
            return true;
        }
        if (connector.getStatus().getConnectorStatus().getAssignedOperator() == null) {
            return true;
        }

        Boolean klb = Resources.delete(
            getKubernetesClient(),
            KameletBinding.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName());
        Boolean secret = Resources.delete(
            getKubernetesClient(),
            Secret.class,
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX);

        LOGGER.debug("deleting connector {}/{} (KameletBinding: {}, Secret: {})",
            connector.getMetadata().getNamespace(),
            connector.getMetadata().getName(),
            klb,
            secret);

        return klb && secret;
    }
}
