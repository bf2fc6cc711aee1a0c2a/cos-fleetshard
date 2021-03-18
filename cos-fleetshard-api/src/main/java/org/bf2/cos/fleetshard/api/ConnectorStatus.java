package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorStatus extends Status {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ObjectReference> resources = new ArrayList<>();

    public List<ObjectReference> getResources() {
        return resources;
    }

    public void setResources(List<ObjectReference> resources) {
        this.resources = resources;
    }

    public enum PhaseType {
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