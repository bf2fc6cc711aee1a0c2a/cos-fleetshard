package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;
import lombok.ToString;

@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class DeployedResource extends ResourceRef {
    private Long deploymentRevision;
    private Long generation;
    private String resourceVersion;

    @JsonProperty
    public Long getDeploymentRevision() {
        return deploymentRevision;
    }

    @JsonProperty
    public void setDeploymentRevision(Long deploymentRevision) {
        this.deploymentRevision = deploymentRevision;
    }

    @JsonProperty
    public Long getGeneration() {
        return generation;
    }

    @JsonProperty
    public void setGeneration(Long generation) {
        this.generation = generation;
    }

    @JsonProperty
    public String getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty
    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @JsonIgnore
    public boolean is(DeployedResource other) {
        return super.equals(other) && Objects.equals(getDeploymentRevision(), other.getDeploymentRevision());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeployedResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DeployedResource resource = (DeployedResource) o;
        return Objects.equals(getDeploymentRevision(), resource.getDeploymentRevision())
            && Objects.equals(getGeneration(), resource.getGeneration())
            && Objects.equals(getResourceVersion(), resource.getResourceVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDeploymentRevision(), getGeneration(), getResourceVersion());
    }
}
