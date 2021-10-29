package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kamelets {

    @JsonProperty("adapter")
    private EndpointKamelet adapter;
    @JsonProperty("kafka")
    private EndpointKamelet kafka;

    @JsonProperty("processors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> processors = new HashMap<>();

    public EndpointKamelet getAdapter() {
        return adapter;
    }

    public void setAdapter(EndpointKamelet adapter) {
        this.adapter = adapter;
    }

    public EndpointKamelet getKafka() {
        return kafka;
    }

    public void setKafka(EndpointKamelet kafka) {
        this.kafka = kafka;
    }

    public Map<String, String> getProcessors() {
        return processors;
    }

    public void setProcessors(Map<String, String> processors) {
        this.processors = processors;
    }
}
