package org.bf2.cos.fleetshard.api.connector;

import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public abstract class ConnectorSpec {
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