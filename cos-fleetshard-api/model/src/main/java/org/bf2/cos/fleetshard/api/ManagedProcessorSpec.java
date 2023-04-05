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

    @PrinterColumn(name = "PROCESSOR_TYPE_ID")
    private String processorTypeId;

    @PrinterColumn(name = "DEPLOYMENT_ID")
    private String deploymentId;

    private Long deploymentResourceVersion;

    private KafkaSpec kafka;

    private String desiredState;

    private String secret;

    private String definition;

    private String unitOfWork;

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

    public String getProcessorTypeId() {
        return processorTypeId;
    }

    public void setProcessorTypeId(String processorTypeId) {
        this.processorTypeId = processorTypeId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public Long getDeploymentResourceVersion() {
        return deploymentResourceVersion;
    }

    public void setDeploymentResourceVersion(Long deploymentResourceVersion) {
        this.deploymentResourceVersion = deploymentResourceVersion;
    }

    public KafkaSpec getKafka() {
        return kafka;
    }

    public void setKafka(KafkaSpec kafka) {
        this.kafka = kafka;
    }

    public String getDesiredState() {
        return desiredState;
    }

    public void setDesiredState(String desiredState) {
        this.desiredState = desiredState;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getUnitOfWork() {
        return unitOfWork;
    }

    public void setUnitOfWork(String unitOfWork) {
        this.unitOfWork = unitOfWork;
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
            && Objects.equals(processorTypeId, that.processorTypeId) && Objects.equals(deploymentId, that.deploymentId)
            && Objects.equals(deploymentResourceVersion, that.deploymentResourceVersion) && Objects.equals(kafka, that.kafka)
            && Objects.equals(desiredState, that.desiredState) && Objects.equals(secret, that.secret)
            && Objects.equals(definition, that.definition) && Objects.equals(unitOfWork, that.unitOfWork)
            && Objects.equals(operatorSelector, that.operatorSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, processorId, processorTypeId, deploymentId, deploymentResourceVersion, kafka,
            desiredState, secret, definition, unitOfWork, operatorSelector);
    }

    @Override
    public String toString() {
        return "ManagedProcessorSpec{" +
            "clusterId='" + clusterId + '\'' +
            ", processorId='" + processorId + '\'' +
            ", processorTypeId='" + processorTypeId + '\'' +
            ", deploymentId='" + deploymentId + '\'' +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", kafka=" + kafka +
            ", desiredState='" + desiredState + '\'' +
            ", secret='" + secret + '\'' +
            ", definition='" + definition + '\'' +
            ", unitOfWork='" + unitOfWork + '\'' +
            ", operatorSelector=" + operatorSelector +
            '}';
    }
}
