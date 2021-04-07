package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusExtractor extends ResourceRef {
    private String conditionsPath = "/status/conditions";
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> conditionTypes = new ArrayList<>();

    public String getConditionsPath() {
        return conditionsPath;
    }

    public void setConditionsPath(String conditionsPath) {
        this.conditionsPath = conditionsPath;
    }

    public List<String> getConditionTypes() {
        return conditionTypes;
    }

    public void setConditionTypes(List<String> conditionTypes) {
        this.conditionTypes = conditionTypes;
    }
}
