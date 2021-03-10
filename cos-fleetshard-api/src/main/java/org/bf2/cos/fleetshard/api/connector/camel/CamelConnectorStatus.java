package org.bf2.cos.fleetshard.api.connector.camel;

import org.bf2.cos.fleetshard.api.connector.ConnectorSpec;

public class CamelConnectorStatus implements ConnectorSpec {
    private String phase;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }
}
