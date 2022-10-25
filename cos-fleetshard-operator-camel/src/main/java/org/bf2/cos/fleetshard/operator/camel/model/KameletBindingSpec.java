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
    private StepEndpoint sink;
    @JsonProperty("source")
    private StepEndpoint source;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("steps")
    private List<StepEndpoint> steps = new ArrayList<>();
    @JsonProperty("errorHandler")
    private ObjectNode errorHandler;

    public ObjectNode getIntegration() {
        return integration;
    }

    public void setIntegration(ObjectNode integration) {
        this.integration = integration;
    }

    public StepEndpoint getSink() {
        return sink;
    }

    public void setSink(StepEndpoint sink) {
        this.sink = sink;
    }

    public StepEndpoint getSource() {
        return source;
    }

    public void setSource(StepEndpoint source) {
        this.source = source;
    }

    public List<StepEndpoint> getSteps() {
        return steps;
    }

    public void setSteps(List<StepEndpoint> steps) {
        this.steps = steps;
    }

    public ObjectNode getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ObjectNode errorHandler) {
        this.errorHandler = errorHandler;
    }
}
