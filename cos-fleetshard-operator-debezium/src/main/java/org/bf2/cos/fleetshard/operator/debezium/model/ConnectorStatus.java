package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;

import io.fabric8.kubernetes.api.model.Condition;

public class ConnectorStatus {

    private final ConnectorStatusSpec statusSpec;
    private final Map<String, Condition> newStatusSpecConditions = new HashMap<>();
    private Boolean connectorReady = null;
    private Condition readyCondition = null;

    public ConnectorStatus(ConnectorStatusSpec statusSpec) {
        this.statusSpec = statusSpec;
    }

    public ConnectorStatusSpec getStatusSpec() {
        return statusSpec;
    }

    public List<Condition> getStatusSpecConditions() {
        List<Condition> previousConditionList = statusSpec.getConditions();
        Map<String, Condition> previousConditions = new HashMap<>(previousConditionList.size());

        for (var condition : previousConditionList) {
            previousConditions.put(condition.getType(), condition);
        }

        for (var condition : newStatusSpecConditions.values()) {
            if (previousConditions.containsKey(condition.getType())) {
                var previousCondition = previousConditions.get(condition.getType());
                if (isSimilarCondition(condition, previousCondition)) {
                    newStatusSpecConditions.put(previousCondition.getType(), previousCondition);
                }
            }
        }
        return new ArrayList<>(newStatusSpecConditions.values());
    }

    private boolean isSimilarCondition(Condition condition, Condition otherCondition) {
        if (condition == null && otherCondition != null) {
            return false;
        }

        assert condition != null;
        if (!Objects.equals(condition.getType(), otherCondition.getType())) {
            return false;
        } else if (!Objects.equals(condition.getStatus(), otherCondition.getStatus())) {
            return false;
        } else if (!Objects.equals(condition.getReason(), otherCondition.getReason())) {
            return false;
        } else {
            return Objects.equals(condition.getMessage(), otherCondition.getMessage());
        }
    }

    public void addCondition(Condition condition) {
        newStatusSpecConditions.put(condition.getType(), condition);
    }

    public Boolean isConnectorReady() {
        return connectorReady;
    }

    public void setConnectorReady(boolean connectorReady) {
        this.connectorReady = connectorReady;
    }

    public Condition readyCondition() {
        return readyCondition;
    }

    public void setReadyCondition(Condition readyCondition) {
        this.readyCondition = readyCondition;
    }
}
