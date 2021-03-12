package org.bf2.cos.fleetshard.api.connector.support;

import java.util.List;

import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(io.fabric8.kubernetes.api.model.Condition.class))
public class Operator {
    private String id;
    private String version;
    private List<Condition> conditions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public enum ConditionType {
        Installing,
        Ready,
        Deleted,
        Error;
    }
}
