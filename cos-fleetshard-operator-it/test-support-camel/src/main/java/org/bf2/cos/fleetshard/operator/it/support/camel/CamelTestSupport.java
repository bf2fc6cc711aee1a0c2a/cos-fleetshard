package org.bf2.cos.fleetshard.operator.it.support.camel;

import java.util.HashMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;

public class CamelTestSupport extends TestSupport {
    static {
        KubernetesDeserializer.registerCustomKind("camel.apache.org/v1alpha1", "KameletBinding", KameletBindingResource.class);
    }

    @JsonDeserialize
    public static class KameletBindingResource extends HashMap<String, Object> implements KubernetesResource {
    }
}
