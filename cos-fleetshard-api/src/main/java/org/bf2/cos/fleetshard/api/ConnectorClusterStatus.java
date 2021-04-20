package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorClusterStatus extends Status {
    private long resourceVersion;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Operator> operators = new ArrayList<>();

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    public enum PhaseType {
        Installing,
        Ready,
        Deleted,
        Error;
    }

    public enum ConditionType {
        Installing,
        Ready,
        Deleted,
        Error;
    }
}
