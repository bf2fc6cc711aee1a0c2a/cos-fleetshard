package org.bf2.cos.fleetshard.api.connector;

import io.fabric8.kubernetes.api.model.ObjectReference;

public class ConnectorDeploymentSpec {
    private String connectorTypeId;
    private Connector<?, ?> connectorSpec;
    private ObjectReference connectorRef;

    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    public void setConnectorTypeId(String connectorTypeId) {
        this.connectorTypeId = connectorTypeId;
    }

    public Connector<?, ?> getConnectorSpec() {
        return connectorSpec;
    }

    public void setConnectorSpec(Connector<?, ?> connectorSpec) {
        this.connectorSpec = connectorSpec;
    }

    public ObjectReference getConnectorRef() {
        return connectorRef;
    }

    public void setConnectorRef(ObjectReference connectorRef) {
        this.connectorRef = connectorRef;
    }
}