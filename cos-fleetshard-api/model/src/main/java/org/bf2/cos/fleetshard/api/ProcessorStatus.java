package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
@JsonPropertyOrder({
    "phase",
    "conditions",
    "deployment",
    "connectorStatus"
})
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessorStatus implements ProcessorDeploymentSpecAware {
    @PrinterColumn
    private PhaseType phase = PhaseType.Initialization;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Condition> conditions = new ArrayList<>();

    private ProcessorDeploymentSpec deployment = new ProcessorDeploymentSpec();
    private ConnectorStatusSpec processorStatusSpec = new ConnectorStatusSpec();

    @Override
    @JsonProperty
    public ProcessorDeploymentSpec getDeployment() {
        return deployment;
    }

    @Override
    @JsonProperty
    public void setDeployment(ProcessorDeploymentSpec deployment) {
        this.deployment = deployment;
    }

    @JsonProperty
    public ConnectorStatusSpec getProcessorStatusSpec() {
        return processorStatusSpec;
    }

    @JsonProperty
    public void setProcessorStatusSpec(ConnectorStatusSpec processorStatusSpec) {
        this.processorStatusSpec = processorStatusSpec;
    }

    @JsonProperty
    public PhaseType getPhase() {
        return phase;
    }

    @JsonProperty
    public void setPhase(PhaseType phase) {
        this.phase = phase;
    }

    @JsonIgnore
    public boolean isInPhase(PhaseType... phases) {
        for (PhaseType type : phases) {
            if (type == phase) {
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
        this.conditions = new ArrayList<>(conditions);
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
