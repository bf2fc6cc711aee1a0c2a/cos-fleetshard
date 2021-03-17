package org.bf2.cos.fleetshard.api.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import org.bf2.cos.fleetshard.api.connector.support.Operator;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorClusterSpec {
    private String connectorClusterId;
    private List<Operator> operators;

    public String getConnectorClusterId() {
        return connectorClusterId;
    }

    public void setConnectorClusterId(String connectorClusterId) {
        this.connectorClusterId = connectorClusterId;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    @Override
    public String toString() {
        return "ConnectorClusterSpec{" +
                "connectorClusterId='" + connectorClusterId + '\'' +
                ", operators=" + operators +
                '}';
    }
}