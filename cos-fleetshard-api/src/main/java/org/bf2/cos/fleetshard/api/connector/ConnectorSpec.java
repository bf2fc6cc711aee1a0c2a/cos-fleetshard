package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(PodTemplateSpec.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorSpec {
    private long resourceVersion;
    private PodTemplateSpec template;

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public PodTemplateSpec getTemplate() {
        return template;
    }

    public void setTemplate(PodTemplateSpec template) {
        this.template = template;
    }
}