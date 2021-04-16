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
    private String kafkaId;
    private Long resourceVersion;
    private Long deploymentResourceVersion;
    private String metaImage;
    private String metaServiceHost;
    private String desiredState;

    public Long getDeploymentResourceVersion() {
        return deploymentResourceVersion;
    }

    public void setDeploymentResourceVersion(Long deploymentResourceVersion) {
        this.deploymentResourceVersion = deploymentResourceVersion;
    }

    @JsonProperty
    public String getKafkaId() {
        return kafkaId;
    }

    @JsonProperty
    public void setKafkaId(String kafkaId) {
        this.kafkaId = kafkaId;
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
    public String getMetaImage() {
        return metaImage;
    }

    @JsonProperty
    public void setMetaImage(String metaImage) {
        this.metaImage = metaImage;
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
    public String getMetaServiceHost() {
        return metaServiceHost;
    }

    @JsonProperty
    public void setMetaServiceHost(String metaServiceHost) {
        this.metaServiceHost = metaServiceHost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentSpec)) {
            return false;
        }
        DeploymentSpec ref = (DeploymentSpec) o;
        return Objects.equals(getKafkaId(), ref.getKafkaId())
            && Objects.equals(getResourceVersion(), ref.getResourceVersion())
            && Objects.equals(getDeploymentResourceVersion(), ref.getDeploymentResourceVersion())
            && Objects.equals(getMetaImage(), ref.getMetaImage())
            && Objects.equals(getMetaServiceHost(), ref.getMetaServiceHost())
            && Objects.equals(getDesiredState(), ref.getDesiredState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getKafkaId(),
            getResourceVersion(),
            getDeploymentResourceVersion(),
            getMetaImage(),
            getMetaServiceHost(),
            getDesiredState());
    }

    @Override
    public String toString() {
        return "ConnectorRef{" +
            "kafkaId='" + kafkaId + '\'' +
            ", resourceVersion=" + resourceVersion +
            ", deploymentResourceVersion=" + deploymentResourceVersion +
            ", metaImage='" + metaImage + '\'' +
            ", metaServiceHost='" + metaServiceHost + '\'' +
            ", desiredState='" + desiredState + '\'' +
            '}';
    }
}
