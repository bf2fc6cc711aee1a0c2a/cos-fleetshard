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
public class ConnectorStatus extends Status {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ResourceRef> resources = new ArrayList<>();

    public List<ResourceRef> getResources() {
        return resources;
    }

    public void setResources(List<ResourceRef> resources) {
        this.resources = resources;
    }

    public enum PhaseType {
        Provisioning,
        Provisioned,
        Installing,
        Ready,
        Deleted,
        Error;
    }

    public enum ConditionType {
        Installing,
        Validating,
        Augmenting,
        Running,
        Paused,
        Deleted,
        Error;
    }
}