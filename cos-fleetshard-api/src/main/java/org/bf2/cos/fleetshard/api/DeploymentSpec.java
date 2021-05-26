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
    private OperatorSelector operatorSelector;

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
    public boolean hasDesiredStateOf(String desiredState) {
        Objects.requireNonNull(desiredState, "desiredState should not be null");
        return Objects.equals(this.desiredState, desiredState);
    }

    @JsonProperty
    public OperatorSelector getOperatorSelector() {
        return operatorSelector;
    }

    @JsonProperty
    public void setOperatorSelector(OperatorSelector operatorSelector) {
        this.operatorSelector = operatorSelector;
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
            && Objects.equals(getDesiredState(), spec.getDesiredState())
            && Objects.equals(getOperatorSelector(), spec.getOperatorSelector());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getResourceVersion(),
            getDeploymentResourceVersion(),
            getDesiredState(),
            getOperatorSelector());
    }

    @Override
    public String toString() {
        return "DeploymentSpec{" +
            "resourceVersion=" + resourceVersion +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", desiredState='" + desiredState + '\'' +
            ", operatorSelector=" + operatorSelector +
            '}';
    }
}
