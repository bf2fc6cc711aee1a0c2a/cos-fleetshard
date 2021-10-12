package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bf2.cos.fleetshard.api.Operator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;

/**
 * Class representing the camel specific shard metadata object.
 */

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamelShardMetadata {
    @JsonProperty("connector_image")
    private String connectorImage;
    @JsonProperty("connector_type")
    private String connectorType;
    @JsonProperty("connector_revision")
    private String connectorRevision;
    @JsonProperty("operators")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Operator> operators = new ArrayList<>();
    @JsonProperty("kamelets")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> kamelets = new HashMap<>();
    @JsonProperty("annotations")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> annotations = new HashMap<>();

    public String getConnectorImage() {
        return connectorImage;
    }

    public void setConnectorImage(String connectorImage) {
        this.connectorImage = connectorImage;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public String getConnectorRevision() {
        return connectorRevision;
    }

    public void setConnectorRevision(String connectorRevision) {
        this.connectorRevision = connectorRevision;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    public Map<String, String> getKamelets() {
        return kamelets;
    }

    public void setKamelets(Map<String, String> kamelets) {
        this.kamelets = kamelets;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CamelShardMetadata)) {
            return false;
        }
        CamelShardMetadata metadata = (CamelShardMetadata) o;
        return Objects.equals(getConnectorImage(), metadata.getConnectorImage())
            && Objects.equals(getConnectorType(), metadata.getConnectorType())
            && Objects.equals(getConnectorRevision(), metadata.getConnectorRevision())
            && Objects.equals(getOperators(), metadata.getOperators())
            && Objects.equals(getKamelets(), metadata.getKamelets());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getConnectorImage(),
            getConnectorType(),
            getConnectorRevision(),
            getOperators(),
            getKamelets());
    }

    @Override
    public String toString() {
        return "CamelShardMetadata{" +
            "connectorImage='" + connectorImage + '\'' +
            ", connectorType='" + connectorType + '\'' +
            ", connectorRevision='" + connectorRevision + '\'' +
            ", operators=" + operators +
            ", kamelets=" + kamelets +
            '}';
    }
}
