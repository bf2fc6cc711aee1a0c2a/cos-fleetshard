package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedProcessorSpec {

    @PrinterColumn(name = "CLUSTER_ID")
    private String clusterId;

    @PrinterColumn(name = "PROCESSOR_ID")
    private String processorId;

    @PrinterColumn(name = "DEPLOYMENT_ID")
    private String deploymentId;
    private ProcessorDeploymentSpec deploymentSpec = new ProcessorDeploymentSpec();
    private OperatorSelector operatorSelector = new OperatorSelector();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public void setProcessorId(String processorId) {
        this.processorId = processorId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public ProcessorDeploymentSpec getDeploymentSpec() {
        return deploymentSpec;
    }

    public void setDeploymentSpec(ProcessorDeploymentSpec deploymentSpec) {
        this.deploymentSpec = deploymentSpec;
    }

    public OperatorSelector getOperatorSelector() {
        return operatorSelector;
    }

    public void setOperatorSelector(OperatorSelector operatorSelector) {
        this.operatorSelector = operatorSelector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManagedProcessorSpec that = (ManagedProcessorSpec) o;
        return Objects.equals(clusterId, that.clusterId) && Objects.equals(processorId, that.processorId)
            && Objects.equals(deploymentId, that.deploymentId) && Objects.equals(deploymentSpec, that.deploymentSpec)
            && Objects.equals(operatorSelector, that.operatorSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, processorId, deploymentId, deploymentSpec, operatorSelector);
    }

    @Override
    public String toString() {
        return "ManagedProcessorSpec{" +
            "clusterId='" + clusterId + '\'' +
            ", processorId='" + processorId + '\'' +
            ", deploymentId='" + deploymentId + '\'' +
            ", deploymentSpec=" + deploymentSpec +
            ", operatorSelector=" + operatorSelector +
            '}';
    }
}
