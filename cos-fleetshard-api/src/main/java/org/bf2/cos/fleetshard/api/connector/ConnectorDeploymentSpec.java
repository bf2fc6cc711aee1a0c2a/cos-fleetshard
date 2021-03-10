package org.bf2.cos.fleetshard.api.connector;

import io.fabric8.kubernetes.api.model.ObjectReference;

public class ConnectorDeploymentSpec {
    private String connectorTypeId;

    /*
     * This is retrieved from the Control Plane
     */
    private Connector<?, ?> connectorSpec;

    /*
     * This should point to the real connector resource and should replace
     * the connectorSpec before persisting to k8s
     */
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