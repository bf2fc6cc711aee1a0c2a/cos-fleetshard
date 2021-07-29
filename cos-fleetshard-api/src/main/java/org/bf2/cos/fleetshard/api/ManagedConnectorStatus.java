package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorStatus {
    @PrinterColumn
    private PhaseType phase = PhaseType.Initialization;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Condition> conditions = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DeployedResource> resources = new ArrayList<>();

    private DeploymentSpec deployment = new DeploymentSpec();
    private ConnectorStatusSpec connectorStatus = new ConnectorStatusSpec();

    private Operator assignedOperator;
    private Operator availableOperator;

    @JsonProperty
    public DeploymentSpec getDeployment() {
        return deployment;
    }

    @JsonProperty
    public void setDeployment(DeploymentSpec deployment) {
        this.deployment = deployment;
    }

    @JsonProperty
    public ConnectorStatusSpec getConnectorStatus() {
        return connectorStatus;
    }

    @JsonProperty
    public void setConnectorStatus(ConnectorStatusSpec connectorStatus) {
        this.connectorStatus = connectorStatus;
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
        this.conditions = conditions;
    }

    @JsonProperty
    public List<DeployedResource> getResources() {
        return resources;
    }

    @JsonProperty
    public void setResources(List<DeployedResource> resources) {
        this.resources = resources;
    }

    public enum PhaseType {
        Initialization,
        Augmentation,
        Monitor,
        Deleting,
        Deleted,
        Stopping,
        Stopped,
        Upgrade,
        Error,
    }

    public enum ConditionType {
        Error,
        Ready
    }

    public enum ConditionStatus {
        True,
        False,
        Unknown
    }
}