package org.bf2.cos.fleetshard.api.connector.camel;

import org.bf2.cos.fleetshard.api.connector.ConnectorSpec;

public class CamelConnectorSpec implements ConnectorSpec {
    private String connectorId;

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }
}
