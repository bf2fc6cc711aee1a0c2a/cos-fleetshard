package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.List;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;

import io.fabric8.kubernetes.api.model.Condition;

public class ConnectorStatus {

    private final ConnectorStatusSpec statusSpec;
    private List<Condition> statusSpecConditions = null;
    private Boolean connectorReady = null;
    private Condition readyCondition = null;

    public ConnectorStatus(ConnectorStatusSpec statusSpec) {
        this.statusSpec = statusSpec;
    }

    public ConnectorStatusSpec getStatusSpec() {
        return statusSpec;
    }

    public List<Condition> getStatusSpecConditions() {
        if (null == statusSpecConditions) {
            statusSpecConditions = statusSpec.getConditions();
        }
        return statusSpecConditions;
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
