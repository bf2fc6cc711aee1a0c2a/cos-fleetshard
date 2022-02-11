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
    "connectorTypeId",
    "connectorResourceVersion",
    "deploymentResourceVersion",
    "desiredState",
    "secret",
    "secretChecksum"
})
public class DeploymentSpec {

    @PrinterColumn(name = "CONNECTOR_TYPE_ID")
    private String connectorTypeId;
    private Long connectorResourceVersion;
    private Long deploymentResourceVersion;
    private KafkaSpec kafka;
    private SchemaRegistrySpec schemaRegistry;
    private String desiredState;
    private String secret;
    private String unitOfWork;

    @JsonProperty
    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    @JsonProperty
    public void setConnectorTypeId(String connectorTypeId) {
        this.connectorTypeId = connectorTypeId;
    }

    @JsonProperty
    public Long getConnectorResourceVersion() {
        return connectorResourceVersion;
    }

    @JsonProperty
    public void setConnectorResourceVersion(Long connectorResourceVersion) {
        this.connectorResourceVersion = connectorResourceVersion;
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
        DeploymentSpec that = (DeploymentSpec) o;
        return Objects.equals(connectorTypeId, that.connectorTypeId)
            && Objects.equals(connectorResourceVersion, that.connectorResourceVersion)
            && Objects.equals(deploymentResourceVersion, that.deploymentResourceVersion)
            && Objects.equals(kafka, that.kafka)
            && Objects.equals(schemaRegistry, that.schemaRegistry)
            && Objects.equals(desiredState, that.desiredState)
            && Objects.equals(secret, that.secret)
            && Objects.equals(unitOfWork, that.unitOfWork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorTypeId,
            connectorResourceVersion,
            deploymentResourceVersion,
            kafka,
            schemaRegistry,
            desiredState,
            secret,
            unitOfWork);
    }

    @Override
    public String toString() {
        return "DeploymentSpec{" +
            "connectorTypeId='" + connectorTypeId + '\'' +
            ", connectorResourceVersion=" + connectorResourceVersion +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", kafka=" + kafka +
            ", schemaRegistry=" + schemaRegistry +
            ", desiredState='" + desiredState + '\'' +
            ", secret='" + secret + '\'' +
            ", unitOfWork='" + unitOfWork + '\'' +
            '}';
    }
}
