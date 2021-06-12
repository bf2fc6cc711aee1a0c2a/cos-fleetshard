package org.bf2.cos.fleetshard.operator.it.support;

import java.util.HashMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;

public class CamelTestSupport {
    static {
        KubernetesDeserializer.registerCustomKind("camel.apache.org/v1alpha1", "KameletBinding", KameletBindingResource.class);
    }

    public static ManagedConnectorOperator newConnectorOperator(
        String namespace,
        String name,
        String version,
        String connectorsMeta) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withNamespace(namespace)
                .withName(name)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withType("camel-connector-operator")
                .withVersion(version)
                .withMetaService(connectorsMeta)
                .build())
            .build();
    }

    @JsonDeserialize
    public static class KameletBindingResource extends HashMap<String, Object> implements KubernetesResource {
    }
}
