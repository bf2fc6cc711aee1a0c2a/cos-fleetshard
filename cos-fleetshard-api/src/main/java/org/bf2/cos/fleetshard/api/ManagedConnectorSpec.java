package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorSpec {
    private String clusterId;
    @PrinterColumn
    private String operatorId;
    @PrinterColumn
    private String connectorId;
    @PrinterColumn
    private String connectorTypeId;
    @PrinterColumn
    private String deploymentId;
    private DeploymentSpec deployment = new DeploymentSpec();
    private OperatorSelector operatorSelector;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    public void setConnectorTypeId(String connectorTypeId) {
        this.connectorTypeId = connectorTypeId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public DeploymentSpec getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentSpec deployment) {
        this.deployment = deployment;
    }

    public OperatorSelector getOperatorSelector() {
        return operatorSelector;
    }

    public void setOperatorSelector(OperatorSelector operatorSelector) {
        this.operatorSelector = operatorSelector;
    }
}