package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DeploymentSpec {
    private Long resourceVersion;
    private Long deploymentResourceVersion;
    private String desiredState;

    public Long getDeploymentResourceVersion() {
        return deploymentResourceVersion;
    }

    public void setDeploymentResourceVersion(Long deploymentResourceVersion) {
        this.deploymentResourceVersion = deploymentResourceVersion;
    }

    @JsonProperty
    public Long getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty
    public void setResourceVersion(Long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @JsonProperty
    public String getDesiredState() {
        return desiredState;
    }

    @JsonProperty
    public void setDesiredState(String desiredState) {
        this.desiredState = desiredState;
    }

    @JsonIgnore
    public boolean hasDesiredStateOf(String... desiredStates) {
        Objects.requireNonNull(desiredStates, "desiredState should not be null");

        for (String desiredState : desiredStates) {
            if (Objects.equals(this.desiredState, desiredState)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentSpec)) {
            return false;
        }
        DeploymentSpec spec = (DeploymentSpec) o;
        return Objects.equals(getResourceVersion(), spec.getResourceVersion())
            && Objects.equals(getDeploymentResourceVersion(), spec.getDeploymentResourceVersion())
            && Objects.equals(getDesiredState(), spec.getDesiredState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getResourceVersion(),
            getDeploymentResourceVersion(),
            getDesiredState());
    }

    @Override
    public String toString() {
        return "DeploymentSpec{" +
            "resourceVersion=" + resourceVersion +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", desiredState='" + desiredState +
            '}';
    }
}
