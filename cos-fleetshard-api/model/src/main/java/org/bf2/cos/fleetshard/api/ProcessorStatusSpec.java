package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessorStatusSpec {

    @PrinterColumn(name = "DEPLOYMENT_PHASE")
    private String phase;
    private List<Condition> conditions = new ArrayList<>();
    private Operator assignedOperator = new Operator();
    private Operator availableOperator = new Operator();

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public Operator getAssignedOperator() {
        return assignedOperator;
    }

    public void setAssignedOperator(Operator assignedOperator) {
        this.assignedOperator = assignedOperator;
    }

    public Operator getAvailableOperator() {
        return availableOperator;
    }

    public void setAvailableOperator(Operator availableOperator) {
        this.availableOperator = availableOperator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorStatusSpec that = (ProcessorStatusSpec) o;
        return Objects.equals(phase, that.phase) && Objects.equals(conditions, that.conditions)
            && Objects.equals(assignedOperator, that.assignedOperator)
            && Objects.equals(availableOperator, that.availableOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase, conditions, assignedOperator, availableOperator);
    }

    @Override
    public String toString() {
        return "ProcessorStatusSpec{" +
            "phase='" + phase + '\'' +
            ", conditions=" + conditions +
            ", assignedOperator=" + assignedOperator +
            ", availableOperator=" + availableOperator +
            '}';
    }
}
