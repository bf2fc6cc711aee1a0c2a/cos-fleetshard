package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingSpecBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
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
    public static final String APP_KUBERNETES_IO_NAME = "app.kubernetes.io/name";
    public static final String APP_KUBERNETES_IO_INSTANCE = "app.kubernetes.io/instance";
    public static final String APP_KUBERNETES_IO_VERSION = "app.kubernetes.io/version";
    public static final String APP_KUBERNETES_IO_COMPONENT = "app.kubernetes.io/component";
    public static final String APP_KUBERNETES_IO_PART_OF = "app.kubernetes.io/part-of";
    public static final String APP_KUBERNETES_IO_MANAGED_BY = "app.kubernetes.io/managed-by";
    public static final String APP_KUBERNETES_IO_CREATED_BY = "app.kubernetes.io/created-by";

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
        ObjectNode connectorSpec,
        KafkaSpec kafkaSpec) {

        final List<CamelOperandSupport.Step> stepDefinitions = createSteps(connectorSpec, shardMetadata);
        final Map<String, String> secretsData = createSecretsData(connector, shardMetadata, connectorSpec, kafkaSpec);

        final String source;
        final String sink;

        switch (shardMetadata.getConnectorType()) {
            case CONNECTOR_TYPE_SOURCE:
                source = shardMetadata.getKamelets().get("connector");
                sink = shardMetadata.getKamelets().get("kafka");
                break;
            case CONNECTOR_TYPE_SINK:
                source = shardMetadata.getKamelets().get("kafka");
                sink = shardMetadata.getKamelets().get("connector");
                break;
            default:
                throw new IllegalArgumentException("Unknown connector type: " + shardMetadata.getConnectorType());
        }

        final Secret secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX)
                .build())
            .addToData(APPLICATION_PROPERTIES, asBytesBase64(secretsData))
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
                        "CONNECTOR_SECRET_CHECKSUM", Secrets.computeChecksum(secret))))
                .withSource(new KameletEndpoint(Kamelet.RESOURCE_API_VERSION, Kamelet.RESOURCE_KIND, source))
                .withSink(new KameletEndpoint(Kamelet.RESOURCE_API_VERSION, Kamelet.RESOURCE_KIND, sink))
                .withErrorHandler(createErrorHandler(connectorSpec))
                .withSteps(
                    stepDefinitions.stream()
                        .map(s -> new KameletEndpoint(
                            Kamelet.RESOURCE_API_VERSION,
                            Kamelet.RESOURCE_KIND,
                            s.templateId,
                            Map.of("id", s.id)))
                        .collect(Collectors.toList()))
                .build())
            .build();

        // Kubernetes recommended labels - should probably move up to abstract and be applied to any type of Connector
        Map<String, String> kameletLabels = KubernetesResourceUtil.getOrCreateLabels(binding);
        ManagedConnectorSpec spec = connector.getSpec();

        kameletLabels.put(APP_KUBERNETES_IO_NAME, spec.getConnectorId());
        kameletLabels.put(APP_KUBERNETES_IO_INSTANCE, spec.getDeploymentId());

        String version = String.valueOf(spec.getDeployment().getDeploymentResourceVersion());
        kameletLabels.put(APP_KUBERNETES_IO_VERSION, version);
        kameletLabels.put(APP_KUBERNETES_IO_COMPONENT, "connector");
        kameletLabels.put(APP_KUBERNETES_IO_PART_OF, spec.getClusterId());

        OperatorSelector operatorSelector = spec.getOperatorSelector();
        String operatorTypeAndId = operatorSelector.getType() + "-" + operatorSelector.getId();
        kameletLabels.put(APP_KUBERNETES_IO_MANAGED_BY, operatorTypeAndId);
        kameletLabels.put(APP_KUBERNETES_IO_CREATED_BY, operatorTypeAndId);

        Map<String, String> annotations = KubernetesResourceUtil.getOrCreateAnnotations(binding);
        if (shardMetadata.getAnnotations() != null) {
            annotations.putAll(shardMetadata.getAnnotations());
        }

        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE, shardMetadata.getConnectorImage());
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON, "false");
        annotations.putIfAbsent(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS, LABELS_TO_TRANSFER);

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
