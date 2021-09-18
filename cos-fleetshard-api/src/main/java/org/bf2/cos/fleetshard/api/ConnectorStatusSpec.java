package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({
    "phase",
    "conditions",
    "resources",
    "assignedOperator",
    "availableOperator"
})
public class ConnectorStatusSpec {
    @PrinterColumn(name = "deployment_phase")
    private String phase;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Condition> conditions = new ArrayList<>();

    private Operator assignedOperator;
    private Operator availableOperator;

    public ConnectorStatusSpec() {
    }

    public ConnectorStatusSpec(String phase, List<Condition> conditions) {
        this.phase = phase;
        this.conditions = new ArrayList<>(conditions);
    }

    public ConnectorStatusSpec(String phase, Condition... conditions) {
        this.phase = phase;
        this.conditions = Arrays.asList(conditions);
    }

    @JsonProperty
    public String getPhase() {
        return phase;
    }

    @JsonProperty
    public void setPhase(String phase) {
        this.phase = phase;
    }

    @JsonIgnore
    public boolean isInPhase(String... phases) {
        for (String phase : phases) {
            if (Objects.equals(this.phase, phase)) {
                return true;
            }
        }

        return false;
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
    public Operator getAssignedOperator() {
        return assignedOperator;
    }

    @JsonProperty
    public void setAssignedOperator(Operator assignedOperator) {
        this.assignedOperator = assignedOperator;
    }

    @JsonProperty
    public Operator getAvailableOperator() {
        return availableOperator;
    }

    @JsonProperty
    public void setAvailableOperator(Operator availableOperator) {
        this.availableOperator = availableOperator;
    }
}
