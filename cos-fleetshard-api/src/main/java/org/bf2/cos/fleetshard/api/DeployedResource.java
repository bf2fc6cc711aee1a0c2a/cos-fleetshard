package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DeployedResource extends ResourceRef {
    private Long deploymentRevision;

    public DeployedResource() {
    }

    public DeployedResource(String apiVersion, String kind, String name) {
        super(apiVersion, kind, name);
    }

    @JsonProperty
    public Long getDeploymentRevision() {
        return deploymentRevision;
    }

    @JsonProperty
    public void setDeploymentRevision(Long deploymentRevision) {
        this.deploymentRevision = deploymentRevision;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
