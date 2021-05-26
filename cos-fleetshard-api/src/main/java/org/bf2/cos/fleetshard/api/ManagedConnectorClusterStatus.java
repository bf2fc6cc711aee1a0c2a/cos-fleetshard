package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorClusterStatus {
    private PhaseType phase;
    private List<Condition> conditions;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Operator> operators = new ArrayList<>();

    @JsonProperty
    public PhaseType getPhase() {
        return phase;
    }

    @JsonProperty
    public void setPhase(PhaseType phase) {
        this.phase = phase;
    }

    @JsonIgnore
    public boolean isInPhase(PhaseType type) {
        return Objects.equals(getPhase(), type);
    }

    @JsonIgnore
    public boolean isReady() {
        return isInPhase(PhaseType.Ready);
    }

    @JsonProperty
    public List<Condition> getConditions() {
        return conditions;
    }

    @JsonProperty
    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @JsonProperty
    public List<Operator> getOperators() {
        return operators;
    }

    @JsonProperty
    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    public enum PhaseType {
        Installing,
        Disconnected,
        Ready,
        Deleted,
        Error;
    }
}
