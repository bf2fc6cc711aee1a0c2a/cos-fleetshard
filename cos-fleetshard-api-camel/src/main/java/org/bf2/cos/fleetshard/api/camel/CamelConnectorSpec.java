package org.bf2.cos.fleetshard.api.camel;

import org.bf2.cos.fleetshard.api.ConnectorSpec;

public class CamelConnectorSpec extends ConnectorSpec {
    private String connectorId;

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }
}
