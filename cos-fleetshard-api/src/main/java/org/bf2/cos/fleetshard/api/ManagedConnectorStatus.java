package org.bf2.cos.fleetshard.api;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bf2.cos.fleet.manager.api.model.meta.StatusExtractor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorStatus {
    private DeploymentSpec deployment = new DeploymentSpec();
    @PrinterColumn
    private PhaseType phase = PhaseType.Initialization;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Condition> conditions = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DeployedResource> resources = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private StatusExtractor statusExtractor = new StatusExtractor();
    private Operator operator;

    @JsonProperty
    public DeploymentSpec getDeployment() {
        return deployment;
    }

    @JsonProperty
    public void setDeployment(DeploymentSpec deployment) {
        this.deployment = deployment;
    }

    @JsonProperty
    public PhaseType getPhase() {
        return phase;
    }

    @JsonProperty
    public void setPhase(PhaseType phase) {
        this.phase = phase;
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

    @JsonProperty
    public StatusExtractor getStatusExtractor() {
        return statusExtractor;
    }

    @JsonProperty
    public void setStatusExtractor(StatusExtractor statusExtractor) {
        this.statusExtractor = statusExtractor;
    }

    @JsonIgnore
    public Condition addCondition(ConditionType type, ConditionStatus status, String message) {
        var answer = new ConditionBuilder()
            .withType(type.name())
            .withStatus(status.name())
            .withReason(type.name())
            .withMessage(message)
            .withLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
            .build();

        this.conditions.add(answer);

        return answer;
    }

    @JsonIgnore
    public DeployedResource addResource(String apiVersion, String kind, String name) {
        return addResource(apiVersion, kind, name, null);
    }

    @JsonIgnore
    public DeployedResource addResource(HasMetadata meta) {
        return addResource(meta, null);
    }

    @JsonIgnore
    public DeployedResource addResource(HasMetadata meta, Long revision) {
        return addResource(meta.getApiVersion(), meta.getKind(), meta.getMetadata().getName(), revision);
    }

    @JsonIgnore
    public DeployedResource addResource(String apiVersion, String kind, String name, Long revision) {
        var answer = new DeployedResource();
        answer.setApiVersion(apiVersion);
        answer.setKind(kind);
        answer.setName(name);
        answer.setDeploymentRevision(revision);

        this.resources.add(answer);

        return answer;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public enum PhaseType {
        Initialization,
        EphemeralMeta,
        EphemeralMetaWatch,
        Augmentation,
        Monitor,
    }

    public enum ConditionType {
        MetaPodCreated,
        MetaPodServiceCreated,
    }

    public enum ConditionStatus {
        True,
        False,
        Unknown
    }
}