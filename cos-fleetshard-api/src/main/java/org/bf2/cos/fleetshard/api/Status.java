package org.bf2.cos.fleetshard.api;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Condition;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(Condition.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Status {
    private String phase;
    private List<Condition> conditions;

    @JsonProperty
    public String getPhase() {
        return phase;
    }

    @JsonProperty
    public void setPhase(String phase) {
        this.phase = phase;
    }

    @JsonIgnore
    public void setPhase(Enum<?> type) {
        setPhase(type.name());
    }

    @JsonIgnore
    public boolean isInPhase(String type) {
        return Objects.equals(getPhase(), type);
    }

    @JsonIgnore
    public boolean isInPhase(Enum<?> type) {
        return Objects.equals(getPhase(), type.name());
    }

    @JsonProperty
    public List<Condition> getConditions() {
        return conditions;
    }

    @JsonProperty
    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }
}
