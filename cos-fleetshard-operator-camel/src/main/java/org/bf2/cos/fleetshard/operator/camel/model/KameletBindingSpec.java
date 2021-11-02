package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class KameletBindingSpec {
    @JsonProperty("integration")
    private ObjectNode integration;
    @JsonProperty("sink")
    private KameletEndpoint sink;
    @JsonProperty("source")
    private KameletEndpoint source;
    @JsonProperty("steps")
    private List<KameletEndpoint> steps = new ArrayList<>();
    @JsonProperty("errorHandler")
    private ObjectNode errorHandler;

    public ObjectNode getIntegration() {
        return integration;
    }

    public void setIntegration(ObjectNode integration) {
        this.integration = integration;
    }

    public KameletEndpoint getSink() {
        return sink;
    }

    public void setSink(KameletEndpoint sink) {
        this.sink = sink;
    }

    public KameletEndpoint getSource() {
        return source;
    }

    public void setSource(KameletEndpoint source) {
        this.source = source;
    }

    public List<KameletEndpoint> getSteps() {
        return steps;
    }

    public void setSteps(List<KameletEndpoint> steps) {
        this.steps = steps;
    }

    public ObjectNode getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ObjectNode errorHandler) {
        this.errorHandler = errorHandler;
    }
}
