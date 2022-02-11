package org.bf2.cos.fleetshard.operator.camel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import groovy.transform.TypeChecked
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.KubernetesClient
import org.bf2.cos.fleetshard.api.KafkaSpec
import org.bf2.cos.fleetshard.api.ManagedConnector
import org.bf2.cos.fleetshard.api.ServiceAccountSpec
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding
import org.mockito.Mockito
import spock.lang.Specification

class Spec extends Specification {

    public static final ObjectMapper JSON = new ObjectMapper();
    public static final YAMLMapper YAML = new YAMLMapper();

    public static final KubernetesClient CLIENT = Mockito.mock(KubernetesClient.class)
    public static final CamelOperandConfiguration CONF =  Mockito.mock(CamelOperandConfiguration.class)
    public static final CamelOperandController CONTROLLER =  new CamelOperandController(CLIENT, CONF)

    // ***********************************
    //
    // Helpers
    //
    // ***********************************

    @TypeChecked
    <T> T readValue(Class<T> type, GString content) {
        return YAML.readValue(content, type)
    }

    def reify(GString connector,  GString meta, GString spec, GString serviceAccount) {
        CONTROLLER.doReify(
                readValue(ManagedConnector.class, connector),
                readValue(CamelShardMetadata.class, meta),
                readValue(ObjectNode.class, spec),
                readValue(ServiceAccountSpec.class, serviceAccount))
    }

    KameletBinding klb(Collection<HasMetadata> resources) {
        return resources.find {
            it.apiVersion == KameletBinding.RESOURCE_API_VERSION && it.kind == KameletBinding.RESOURCE_KIND
        } as KameletBinding
    }


    Secret secret(Collection<HasMetadata> resources) {
        return resources.find {
            it.apiVersion == 'v1' && it.kind == 'Secret'
        } as Secret
    }
}
