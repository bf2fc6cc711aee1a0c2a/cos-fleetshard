package org.bf2.cos.fleetshard.operator.camel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kamelets {

    @JsonProperty("adapter")
    private EndpointKamelet adapter;
    @JsonProperty("kafka")
    private EndpointKamelet kafka;

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

}
