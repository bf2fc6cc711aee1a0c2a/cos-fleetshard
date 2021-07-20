package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestSupport;

public class CamelConnectorStatusTestSupport extends CamelTestSupport {
    protected Map<String, Object> updateKameletBinding(String name) {
        return updateUnstructured("camel.apache.org/v1alpha1", "KameletBinding", name, binding -> {
            binding.with("status").put("phase", "Ready");
            binding.with("status").withArray("conditions")
                .addObject()
                .put("message", "a message")
                .put("reason", "a reason")
                .put("status", "the status")
                .put("type", "the type")
                .put("lastTransitionTime", "2021-06-12T12:35:09+02:00");
        });
    }
}
