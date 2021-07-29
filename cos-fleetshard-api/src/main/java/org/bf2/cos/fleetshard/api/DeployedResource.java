package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.sundr.builder.annotations.Buildable;
import lombok.ToString;

@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class DeployedResource extends ResourceRef {
    private Long deploymentRevision;

    public DeployedResource() {
    }

    public DeployedResource(String apiVersion, String kind, String name) {
        this(apiVersion, kind, name, null);
    }

    public DeployedResource(String apiVersion, String kind, String name, Long deploymentRevision) {
        super(apiVersion, kind, name);

        this.deploymentRevision = deploymentRevision;
    }

    public DeployedResource(HasMetadata resource, Long deploymentRevision) {
        this(
            resource.getApiVersion(),
            resource.getKind(),
            resource.getMetadata().getName(),
            deploymentRevision);
    }

    public DeployedResource(HasMetadata resource) {
        this(
            resource.getApiVersion(),
            resource.getKind(),
            resource.getMetadata().getName(),
            null);
    }

    @JsonProperty
    public Long getDeploymentRevision() {
        return deploymentRevision;
    }

    @JsonProperty
    public void setDeploymentRevision(Long deploymentRevision) {
        this.deploymentRevision = deploymentRevision;
    }

    @JsonIgnore
    public boolean is(String apiVersion, String kind, String name, Long deploymentRevision) {
        Objects.requireNonNull(deploymentRevision, "deploymentRevision");

        return super.is(apiVersion, kind, name)
            && Objects.equals(this.deploymentRevision, deploymentRevision);
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
        return Objects.equals(getDeploymentRevision(), resource.getDeploymentRevision());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDeploymentRevision());
    }
}
