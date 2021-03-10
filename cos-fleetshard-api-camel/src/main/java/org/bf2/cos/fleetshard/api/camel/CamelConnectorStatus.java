package org.bf2.cos.fleetshard.api.camel;

import org.bf2.cos.fleetshard.api.ConnectorSpec;

public class CamelConnectorStatus extends ConnectorSpec {
    private String phase;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }
}
