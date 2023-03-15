package org.bf2.cos.fleetshard.operator.debezium.model;

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
    @JsonProperty("container_image")
    private String containerImage;
    @JsonProperty("connector_class")
    private String connectorClass;

    @JsonProperty("operators")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Operator> operators = new ArrayList<>();

    public String getContainerImage() {
        return containerImage;
    }

    public void setContainerImage(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getConnectorClass() {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass) {
        this.connectorClass = connectorClass;
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
        return Objects.equals(getContainerImage(), metadata.getContainerImage())
            && Objects.equals(getConnectorClass(), metadata.getConnectorClass())
            && Objects.equals(getOperators(), metadata.getOperators());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getContainerImage(),
            getConnectorClass(),
            getOperators());
    }
}
