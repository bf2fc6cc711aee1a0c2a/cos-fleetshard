package org.bf2.cos.fleetshard.api.cluster;

import java.util.List;

public class ConnectorClusterStatus {
    private List<ConnectorClusterCondition> conditions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ConnectorClusterCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConnectorClusterCondition> conditions) {
        this.conditions = conditions;
    }
}
