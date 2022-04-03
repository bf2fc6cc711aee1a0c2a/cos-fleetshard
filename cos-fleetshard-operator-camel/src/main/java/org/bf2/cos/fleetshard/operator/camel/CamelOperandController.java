package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.camel.CamelOperandConfiguration.Health;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingSpec;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.APPLICATION_PROPERTIES;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.KAMEL_OPERATOR_ID;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_ID_PLACEHOLDER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.SA_CLIENT_SECRET_PLACEHOLDER;
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
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.configureKameletProperties;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createErrorHandler;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createIntegrationSpec;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSecretsData;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSteps;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.hasSchemaRegistry;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.lookupBinding;
import static org.bf2.cos.fleetshard.support.CollectionUtils.asBytesBase64;

@Singleton
public class CamelOperandController extends AbstractOperandController<CamelShardMetadata, ObjectNode, ObjectNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelOperandController.class);

    private final CamelOperandConfiguration configuration;

    public CamelOperandController(KubernetesClient kubernetesClient, CamelOperandConfiguration configuration) {
        super(kubernetesClient, CamelShardMetadata.class, ObjectNode.class, ObjectNode.class);

        this.configuration = configuration;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return List.of(KameletBinding.RESOURCE_DEFINITION);
    }

    @SuppressFBWarnings("HARD_CODE_PASSWORD")
    @Override
    protected List<HasMetadata> doReify(
        ManagedConnector connector,
        CamelShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, ObjectNode> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        final Map<String, String> properties = createSecretsData(
            connector,
            connectorConfiguration,
            serviceAccountSpec,
            configuration);

        final ObjectNode errorHandler = createErrorHandler(
            connector,
            connectorConfiguration.getErrorHandlerSpec());

        final List<KameletEndpoint> stepDefinitions;
        final KameletEndpoint source;
        final KameletEndpoint sink;

        //
        // - source:
        //     ref:
        //       apiVersion: camel.apache.org/v1alpha1
        //       kind: Kamelet
        //       name: adapter-source
        //     properties:
        //       id: foo
        //       key: 'val'
        //       foo_secret: {{prefix_foo_secret}}
        // - sink:
        //     ref:
        //       apiVersion: camel.apache.org/v1alpha1
        //       kind: Kamelet
        //       name: cos-kafka-sink
        //     properties:
        //       id: bar
        //       key: 'val'
        //       bar_secret: {{prefix_bar_secret}}
        //
        //
        switch (shardMetadata.getConnectorType()) {
            case CONNECTOR_TYPE_SOURCE:
                source = KameletEndpoint.kamelet(shardMetadata.getKamelets().getAdapter().getName());
                source.getProperties().put("id", connector.getSpec().getDeploymentId() + "-source");

                configureKameletProperties(
                    source.getProperties(),
                    connectorConfiguration.getConnectorSpec(),
                    shardMetadata.getKamelets().getAdapter());

                sink = KameletEndpoint.kamelet(shardMetadata.getKamelets().getKafka().getName());
                sink.getProperties().put("id", connector.getSpec().getDeploymentId() + "-sink");
                sink.getProperties().put("bootstrapServers", connector.getSpec().getDeployment().getKafka().getUrl());
                sink.getProperties().put("user", SA_CLIENT_ID_PLACEHOLDER);
                sink.getProperties().put("password", SA_CLIENT_SECRET_PLACEHOLDER);

                configureKameletProperties(
                    sink.getProperties(),
                    connectorConfiguration.getConnectorSpec(),
                    shardMetadata.getKamelets().getKafka());

                if (hasSchemaRegistry(connector)) {
                    sink.getProperties().put(
                        "registryUrl",
                        connector.getSpec().getDeployment().getSchemaRegistry().getUrl());
                }

                stepDefinitions = createSteps(
                    connector,
                    connectorConfiguration,
                    shardMetadata,
                    sink);
                break;
            case CONNECTOR_TYPE_SINK:
                source = KameletEndpoint.kamelet(shardMetadata.getKamelets().getKafka().getName());
                source.getProperties().put("id", connector.getSpec().getDeploymentId() + "-source");
                source.getProperties().put("consumerGroup", connector.getSpec().getDeploymentId());
                source.getProperties().put("bootstrapServers", connector.getSpec().getDeployment().getKafka().getUrl());
                source.getProperties().put("user", SA_CLIENT_ID_PLACEHOLDER);
                source.getProperties().put("password", SA_CLIENT_SECRET_PLACEHOLDER);

                configureKameletProperties(
                    source.getProperties(),
                    connectorConfiguration.getConnectorSpec(),
                    shardMetadata.getKamelets().getKafka());

                if (hasSchemaRegistry(connector)) {
                    source.getProperties().put(
                        "registryUrl",
                        connector.getSpec().getDeployment().getSchemaRegistry().getUrl());
                }

                sink = KameletEndpoint.kamelet(shardMetadata.getKamelets().getAdapter().getName());
                sink.getProperties().put("id", connector.getSpec().getDeploymentId() + "-sink");

                configureKameletProperties(
                    sink.getProperties(),
                    connectorConfiguration.getConnectorSpec(),
                    shardMetadata.getKamelets().getAdapter());

                stepDefinitions = createSteps(
                    connector,
                    connectorConfiguration,
                    shardMetadata,
                    source);
                break;
            default:
                throw new IllegalArgumentException("Unknown connector type: " + shardMetadata.getConnectorType());
        }

        final Secret secret = new Secret();
        secret.setMetadata(new ObjectMeta());
        secret.getMetadata().setName(connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX);
        secret.setData(Map.of(APPLICATION_PROPERTIES, asBytesBase64(properties)));

        final ObjectNode integration = createIntegrationSpec(
            secret.getMetadata().getName(),
            configuration,
            Map.of(
                "CONNECTOR_SECRET_NAME", secret.getMetadata().getName(),
                "CONNECTOR_SECRET_CHECKSUM", Secrets.computeChecksum(secret),
                "CONNECTOR_ID", connector.getSpec().getConnectorId(),
                "CONNECTOR_DEPLOYMENT_ID", connector.getSpec().getDeploymentId()));

        final KameletBinding binding = new KameletBinding();
        binding.setMetadata(new ObjectMeta());
        binding.getMetadata().setName(connector.getMetadata().getName());
        binding.getMetadata().setAnnotations(new TreeMap<>());
        binding.setSpec(new KameletBindingSpec());
        binding.getSpec().setSource(source);
        binding.getSpec().setSink(sink);
        binding.getSpec().setErrorHandler(errorHandler);
        binding.getSpec().setSteps(stepDefinitions);
        binding.getSpec().setIntegration(integration);

        Map<String, String> annotations = binding.getMetadata().getAnnotations();
        if (shardMetadata.getAnnotations() != null) {
            annotations.putAll(shardMetadata.getAnnotations());
        }

        if (configuration.labelSelection().enabled()) {
            Operator assigned = connector.getStatus().getConnectorStatus().getAssignedOperator();
            if (assigned != null && assigned.getId() != null) {
                annotations.putIfAbsent(KAMEL_OPERATOR_ID, assigned.getId());
            }
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
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_SUCCESS_THRESHOLD,
                health.readinessSuccessThreshold());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_FAILURE_THRESHOLD,
                health.readinessFailureThreshold());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PERIOD,
                health.readinessPeriodSeconds());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_TIMEOUT,
                health.readinessTimeoutSeconds());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_SUCCESS_THRESHOLD,
                health.livenessSuccessThreshold());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_FAILURE_THRESHOLD,
                health.livenessFailureThreshold());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PERIOD,
                health.livenessPeriodSeconds());
            annotations.putIfAbsent(
                TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_TIMEOUT,
                health.livenessTimeoutSeconds());
        }

        return List.of(secret, binding);
    }

    @Override
    public void status(ManagedConnector connector) {
        lookupBinding(getKubernetesClient(), connector).ifPresent(
            klb -> computeStatus(connector.getStatus().getConnectorStatus(), klb.getStatus()));
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
