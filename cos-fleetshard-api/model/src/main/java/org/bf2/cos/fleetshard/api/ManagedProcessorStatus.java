package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedProcessorStatus {

    @PrinterColumn(name = "PHASE")
    private PhaseType phase = PhaseType.Initialization;
    private List<Condition> conditions = new ArrayList<>();
    private ProcessorDeploymentSpec deployment = new ProcessorDeploymentSpec();
    private ProcessorStatusSpec processorStatus = new ProcessorStatusSpec();

    public PhaseType getPhase() {
        return phase;
    }

    public void setPhase(PhaseType phase) {
        this.phase = phase;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public ProcessorDeploymentSpec getDeployment() {
        return deployment;
    }

    public void setDeployment(ProcessorDeploymentSpec deployment) {
        this.deployment = deployment;
    }

    public ProcessorStatusSpec getProcessorStatus() {
        return processorStatus;
    }

    public void setProcessorStatus(ProcessorStatusSpec processorStatus) {
        this.processorStatus = processorStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManagedProcessorStatus that = (ManagedProcessorStatus) o;
        return phase == that.phase && Objects.equals(conditions, that.conditions) && Objects.equals(deployment, that.deployment)
            && Objects.equals(processorStatus, that.processorStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase, conditions, deployment, processorStatus);
    }

    @Override
    public String toString() {
        return "ManagedProcessorStatus{" +
            "phase=" + phase +
            ", conditions=" + conditions +
            ", deployment=" + deployment +
            ", processorStatus=" + processorStatus +
            '}';
    }

    public enum PhaseType {
        Initialization,
        Augmentation,
        Monitor,
        Deleting,
        Deleted,
        Stopping,
        Stopped,
        Transferring,
        Transferred,
        Error;

        private final String id;

        PhaseType() {
            this.id = name().toLowerCase(Locale.US);
        }

        public String getId() {
            return id;
        }
    }
}
