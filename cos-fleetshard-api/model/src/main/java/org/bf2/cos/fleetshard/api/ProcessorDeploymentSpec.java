package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessorDeploymentSpec {

    @PrinterColumn(name = "PROCESSOR_TYPE_ID")
    private String processorTypeId;
    private Long deploymentResourceVersion;
    private KafkaSpec kafka;
    private String desiredState;
    private String secret;
    private String configMap;
    private String unitOfWork;

    public String getProcessorTypeId() {
        return processorTypeId;
    }

    public void setProcessorTypeId(String processorTypeId) {
        this.processorTypeId = processorTypeId;
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

    public String getConfigMap() {
        return configMap;
    }

    public void setConfigMap(String configMap) {
        this.configMap = configMap;
    }

    public String getUnitOfWork() {
        return unitOfWork;
    }

    public void setUnitOfWork(String unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDeploymentSpec that = (ProcessorDeploymentSpec) o;
        return Objects.equals(processorTypeId, that.processorTypeId)
            && Objects.equals(deploymentResourceVersion, that.deploymentResourceVersion) && Objects.equals(kafka, that.kafka)
            && Objects.equals(desiredState, that.desiredState) && Objects.equals(secret, that.secret)
            && Objects.equals(configMap, that.configMap) && Objects.equals(unitOfWork, that.unitOfWork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processorTypeId, deploymentResourceVersion, kafka, desiredState, secret, configMap, unitOfWork);
    }

    @Override
    public String toString() {
        return "ProcessorDeploymentSpec{" +
            "processorTypeId='" + processorTypeId + '\'' +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", kafka=" + kafka +
            ", desiredState='" + desiredState + '\'' +
            ", secret='" + secret + '\'' +
            ", configMap='" + configMap + '\'' +
            ", unitOfWork='" + unitOfWork + '\'' +
            '}';
    }
}
