package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(PodTemplateSpec.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorSpec {
    private long resourceVersion;
    private KafkaSpec kafka;
    private PodTemplateSpec template;

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public KafkaSpec getKafka() {
        return kafka;
    }

    public void setKafka(KafkaSpec kafka) {
        this.kafka = kafka;
    }

    public PodTemplateSpec getTemplate() {
        return template;
    }

    public void setTemplate(PodTemplateSpec template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return "ConnectorSpec{" +
                "resourceVersion=" + resourceVersion +
                ", kafka=" + kafka +
                ", template=" + template +
                '}';
    }
}