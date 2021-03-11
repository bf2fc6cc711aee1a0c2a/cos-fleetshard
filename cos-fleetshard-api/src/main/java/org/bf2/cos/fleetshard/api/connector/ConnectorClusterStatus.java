package org.bf2.cos.fleetshard.api.connector;

import java.util.List;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.connector.support.Condition;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class ConnectorClusterStatus {
    private long resourceVersion;
    private List<Condition> conditions;

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public Optional<Condition> getLatestCondition() {
        return conditions != null
                ? Optional.of(conditions.get(conditions.size() - 1))
                : Optional.empty();
    };

    public enum ConditionType {
        Installing,
        Ready,
        Deleted,
        Error;
    }
}
