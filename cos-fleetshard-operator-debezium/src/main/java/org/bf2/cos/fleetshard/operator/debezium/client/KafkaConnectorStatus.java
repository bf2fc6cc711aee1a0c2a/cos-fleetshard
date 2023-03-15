package org.bf2.cos.fleetshard.operator.debezium.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class KafkaConnectorStatus {
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_PAUSED = "PAUSED";
    public static final String STATE_UNASSIGNED = "UNASSIGNED";
    public static final String STATE_RUNNING = "RUNNING";

    @JsonProperty
    public String state;

    @JsonProperty
    public String trace;

    public KafkaConnectorStatus() {
    }

    public KafkaConnectorStatus(String state, String trace) {
        this.state = state;
        this.trace = trace;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}
