package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({
    "processorTypeId",
    "connectorResourceVersion",
    "deploymentResourceVersion",
    "desiredState",
    "secret",
    "configMapChecksum",
    "unitOfWork"
})
public class ProcessorDeploymentSpec {

    @PrinterColumn(name = "PROCESSOR_TYPE_ID")
    private String processorTypeId;
    private Long processorResourceVersion;
    private Long deploymentResourceVersion;
    private KafkaSpec kafka;
    private SchemaRegistrySpec schemaRegistry;
    private String desiredState;
    private String secret;
    private String configMapChecksum;
    private String unitOfWork;

    @JsonProperty
    public String getProcessorTypeId() {
        return processorTypeId;
    }

    @JsonProperty
    public void setProcessorTypeId(String connectorTypeId) {
        this.processorTypeId = connectorTypeId;
    }

    @JsonProperty
    public Long getProcessorResourceVersion() {
        return processorResourceVersion;
    }

    @JsonProperty
    public void setProcessorResourceVersion(Long processorResourceVersion) {
        this.processorResourceVersion = processorResourceVersion;
    }

    @JsonProperty
    public Long getDeploymentResourceVersion() {
        return deploymentResourceVersion;
    }

    @JsonProperty
    public void setDeploymentResourceVersion(Long deploymentResourceVersion) {
        this.deploymentResourceVersion = deploymentResourceVersion;
    }

    @JsonProperty
    public KafkaSpec getKafka() {
        return kafka;
    }

    @JsonProperty
    public void setKafka(KafkaSpec kafka) {
        this.kafka = kafka;
    }

    @JsonProperty
    public SchemaRegistrySpec getSchemaRegistry() {
        return schemaRegistry;
    }

    @JsonProperty
    public void setSchemaRegistry(SchemaRegistrySpec schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @JsonProperty
    public String getDesiredState() {
        return desiredState;
    }

    @JsonProperty
    public void setDesiredState(String desiredState) {
        this.desiredState = desiredState;
    }

    @JsonProperty
    public String getSecret() {
        return secret;
    }

    @JsonProperty
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @JsonProperty
    public String getConfigMapChecksum() {
        return configMapChecksum;
    }

    @JsonProperty
    public void setConfigMapChecksum(String configMapChecksum) {
        this.configMapChecksum = configMapChecksum;
    }

    @JsonProperty
    public String getUnitOfWork() {
        return unitOfWork;
    }

    @JsonProperty
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
            && Objects.equals(processorResourceVersion, that.processorResourceVersion)
            && Objects.equals(deploymentResourceVersion, that.deploymentResourceVersion)
            && Objects.equals(kafka, that.kafka)
            && Objects.equals(schemaRegistry, that.schemaRegistry)
            && Objects.equals(desiredState, that.desiredState)
            && Objects.equals(secret, that.secret)
            && Objects.equals(configMapChecksum, that.configMapChecksum)
            && Objects.equals(unitOfWork, that.unitOfWork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processorTypeId,
            processorResourceVersion,
            deploymentResourceVersion,
            kafka,
            schemaRegistry,
            desiredState,
            secret,
            configMapChecksum,
            unitOfWork);
    }

    @Override
    public String toString() {
        return "DeploymentSpec{" +
            "processorTypeId='" + processorTypeId + '\'' +
            ", processorResourceVersion=" + processorResourceVersion +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", kafka=" + kafka +
            ", schemaRegistry=" + schemaRegistry +
            ", desiredState='" + desiredState + '\'' +
            ", secret='" + secret + '\'' +
            ", configMapChecksum='" + configMapChecksum + '\'' +
            ", unitOfWork='" + unitOfWork + '\'' +
            '}';
    }
}
