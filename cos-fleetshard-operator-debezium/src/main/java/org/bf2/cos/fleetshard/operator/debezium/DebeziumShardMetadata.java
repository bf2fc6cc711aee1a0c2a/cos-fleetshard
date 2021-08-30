package org.bf2.cos.fleetshard.operator.debezium;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bf2.cos.fleetshard.api.Operator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;

/**
 * Class representing the debezium specific shard metadata object.
 */
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumShardMetadata {
    @JsonProperty("connector_image")
    private String connectorImage;
    @JsonProperty("connector_class")
    private String connectorClass;
    @JsonProperty("connector_name")
    private String connectorName;
    @JsonProperty("connector_version")
    private String connectorVersion;
    @JsonProperty("connector_sha512sum")
    private String connectorSha512sum;

    @JsonProperty("operators")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Operator> operators = new ArrayList<>();

    public String getConnectorImage() {
        return connectorImage;
    }

    public void setConnectorImage(String connectorImage) {
        this.connectorImage = connectorImage;
    }

    public String getConnectorClass() {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass) {
        this.connectorClass = connectorClass;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getConnectorVersion() {
        return connectorVersion;
    }

    public void setConnectorVersion(String connectorVersion) {
        this.connectorVersion = connectorVersion;
    }

    public String getConnectorSha512sum() {
        return connectorSha512sum;
    }

    public void setConnectorSha512sum(String connectorSha512sum) {
        this.connectorSha512sum = connectorSha512sum;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DebeziumShardMetadata)) {
            return false;
        }
        DebeziumShardMetadata metadata = (DebeziumShardMetadata) o;
        return Objects.equals(getConnectorImage(), metadata.getConnectorImage())
            && Objects.equals(getConnectorClass(), metadata.getConnectorClass())
            && Objects.equals(getConnectorName(), metadata.getConnectorName())
            && Objects.equals(getConnectorVersion(), metadata.getConnectorVersion())
            && Objects.equals(getConnectorSha512sum(), metadata.getConnectorSha512sum())
            && Objects.equals(getOperators(), metadata.getOperators());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getConnectorImage(),
            getConnectorClass(),
            getConnectorName(),
            getConnectorVersion(),
            getConnectorSha512sum(),
            getOperators());
    }
}
