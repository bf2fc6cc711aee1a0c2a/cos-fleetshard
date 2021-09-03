package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingSpecBuilder;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus;
import org.bf2.cos.fleetshard.operator.camel.model.KameletEndpoint;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.bf2.cos.fleetshard.support.resources.UnstructuredSupport;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ANNOTATIONS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.APPLICATION_PROPERTIES;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS;
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.computeStatus;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createIntegrationSpec;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSecretsData;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.createSteps;
import static org.bf2.cos.fleetshard.operator.camel.CamelOperandSupport.lookupBinding;
import static org.bf2.cos.fleetshard.support.CollectionUtils.asBytesBase64;
import static org.bf2.cos.fleetshard.support.resources.Resources.ANNOTATION_DELETION_MODE;
import static org.bf2.cos.fleetshard.support.resources.Resources.DELETION_MODE_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Resources.DELETION_MODE_DEPLOYMENT;

@Singleton
public class CamelOperandController extends AbstractOperandController<CamelShardMetadata, ObjectNode> {
    private final CamelOperandConfiguration configuration;

    public CamelOperandController(UnstructuredClient uc, CamelOperandConfiguration configuration) {
        super(uc, CamelShardMetadata.class, ObjectNode.class);

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
        final Map<String, String> secretsData = createSecretsData(shardMetadata, connectorSpec, kafkaSpec);

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
                .withName(connector.getMetadata().getName() + Connectors.CONNECTOR_SECRET_SUFFIX)
                .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_CONNECTOR)
                .build())
            .addToData(APPLICATION_PROPERTIES, asBytesBase64(secretsData))
            .build();

        final KameletBinding binding = new KameletBindingBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(connector.getMetadata().getName())
                .addToAnnotations(ANNOTATION_DELETION_MODE, DELETION_MODE_DEPLOYMENT)
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE, shardMetadata.getConnectorImage())
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED, "false")
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED, "false")
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON, "false")
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS, LABELS_TO_TRANSFER)
                .addToAnnotations(TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS, ANNOTATIONS_TO_TRANSFER)
                .build())
            .withSpec(new KameletBindingSpecBuilder()
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
                .build())
            .build();

        return List.of(secret, binding);
    }

    @Override
    public void status(ManagedConnector connector) {
        lookupBinding(getUnstructuredClient(), connector)
            .flatMap(kb -> UnstructuredSupport.getPropertyAs(kb, "status", KameletBindingStatus.class))
            .ifPresent(kbs -> computeStatus(connector.getStatus().getConnectorStatus(), kbs));
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        return delete(connector);
    }
}
