package org.bf2.cos.fleetshard.api.connector;

import java.util.List;

import org.bf2.cos.fleetshard.api.connector.support.Operator;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class ConnectorClusterSpec {
    private List<Operator> operators;

    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }
}