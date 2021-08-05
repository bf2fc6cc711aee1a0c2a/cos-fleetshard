package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingSpecBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.bf2.cos.fleetshard.support.resources.UnstructuredSupport;

import static org.bf2.cos.fleetshard.api.ManagedConnector.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DELETION_MODE_DEPLOYMENT;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createApplicationProperties;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createIntegrationSpec;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSteps;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.isKameletBinding;
import static org.bf2.cos.fleetshard.support.PropertiesUtil.asBytesBase64;

@Singleton
public class CamelOperandController extends AbstractOperandController<CamelShardMetadata, ObjectNode> {
    private final UnstructuredClient uc;
    private final CamelOperandConfiguration configuration;

    public CamelOperandController(UnstructuredClient uc, CamelOperandConfiguration configuration) {
        super(CamelShardMetadata.class, ObjectNode.class);

        this.uc = uc;
        this.configuration = configuration;
    }

    @Override
    public List<CustomResourceDefinitionContext> getResourceTypes() {
        return List.of(KameletBinding.RESOURCE_DEFINITION);
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnectorSpec connector,
        CamelShardMetadata shardMetadata,
        ObjectNode connectorSpec,
        KafkaSpec kafkaSpec) {

        final String name = connector.getId() + "-camel";
        final List<CamelOperandSupport.Step> stepDefinitions = createSteps(connectorSpec, shardMetadata);
        final Properties secretsData = createApplicationProperties(shardMetadata, connectorSpec, kafkaSpec);

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

        final Secret secret = new Secret();
        secret.setMetadata(new ObjectMetaBuilder()
            .withName(name + "-" + connector.getDeployment().getDeploymentResourceVersion())
            .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_DEPLOYMENT)
            .build());
        secret.setImmutable(true);
        secret.setData(Map.of("application.properties", asBytesBase64(secretsData)));

        final KameletBinding binding = new KameletBinding();
        binding.setMetadata(new ObjectMetaBuilder()
            .withName(name)
            .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_CONNECTOR)
            .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE, shardMetadata.getConnectorImage())
            .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED, "false")
            .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED, "false")
            .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON, "false")
            .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS, LABELS_TO_TRANSFER)
            .build());
        binding.setSpec(new KameletBindingSpecBuilder()
            .withIntegration(createIntegrationSpec(secret.getMetadata().getName(), configuration))
            .withSource(new KameletEndpoint(Kamelet.RESOURCE_API_VERSION, Kamelet.RESOURCE_KIND, source))
            .withSink(new KameletEndpoint(Kamelet.RESOURCE_API_VERSION, Kamelet.RESOURCE_KIND, sink))
            .withSteps(
                stepDefinitions.stream()
                    .map(s -> new KameletEndpoint(
                        Kamelet.RESOURCE_API_VERSION,
                        Kamelet.RESOURCE_KIND,
                        s.templateId,
                        Map.of("id", s.id)))
                    .collect(Collectors.toList()))
            .build());

        return List.of(secret, binding);
    }

    @Override
    public void status(ManagedConnectorStatus connector) {
        if (connector.getResources() == null) {
            return;
        }

        for (ResourceRef ref : connector.getResources()) {
            GenericKubernetesResource resource = uc.get(ref.getNamespace(), ref);

            if (resource == null) {
                continue;
            }
            if (!isKameletBinding(ref)) {
                continue;
            }

            UnstructuredSupport.getPropertyAs(resource, "status", KameletBindingStatus.class).ifPresent(status -> {
                ConnectorStatusSpec statusSpec = new ConnectorStatusSpec();

                if (status.phase != null) {
                    switch (status.phase.toLowerCase(Locale.US)) {
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

                if (status.conditions != null) {
                    statusSpec.setConditions(status.conditions);
                }

                connector.setConnectorStatus(statusSpec);
            });
        }
    }

}
