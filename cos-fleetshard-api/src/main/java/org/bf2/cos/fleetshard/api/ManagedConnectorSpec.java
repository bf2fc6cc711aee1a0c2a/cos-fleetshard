package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({
    "clusterId",
    "connectorId",
    "deploymentId",
    "deployment",
    "operatorSelector"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorSpec {
    @PrinterColumn
    private String clusterId;

    @PrinterColumn
    private String connectorId;

    @PrinterColumn
    private String deploymentId;
    private DeploymentSpec deployment = new DeploymentSpec();

    private OperatorSelector operatorSelector;

    @JsonProperty
    public String getClusterId() {
        return clusterId;
    }

    @JsonProperty
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @JsonProperty
    public String getConnectorId() {
        return connectorId;
    }

    @JsonProperty
    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    @JsonProperty
    public String getDeploymentId() {
        return deploymentId;
    }

    @JsonProperty
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @JsonProperty
    public DeploymentSpec getDeployment() {
        return deployment;
    }

    @JsonProperty
    public void setDeployment(DeploymentSpec deployment) {
        this.deployment = deployment;
    }

    @JsonProperty
    public OperatorSelector getOperatorSelector() {
        return operatorSelector;
    }

    @JsonProperty
    public void setOperatorSelector(OperatorSelector operatorSelector) {
        this.operatorSelector = operatorSelector;
    }
}