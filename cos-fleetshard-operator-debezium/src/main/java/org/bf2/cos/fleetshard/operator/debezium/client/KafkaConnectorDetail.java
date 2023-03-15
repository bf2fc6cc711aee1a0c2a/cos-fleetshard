package org.bf2.cos.fleetshard.operator.debezium.client;

import java.util.List;

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
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = {})
public class KafkaConnectorDetail {
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_PAUSED = "PAUSED";
    public static final String STATE_UNASSIGNED = "UNASSIGNED";
    public static final String STATE_RUNNING = "RUNNING";

    @JsonProperty
    public String name;

    @JsonProperty
    public String type;

    @JsonProperty
    public KafkaConnectorStatus connector;

    @JsonProperty
    public List<KafkaConnectorTask> tasks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KafkaConnectorStatus getConnector() {
        return connector;
    }

    public void setConnector(KafkaConnectorStatus connector) {
        this.connector = connector;
    }

    public List<KafkaConnectorTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<KafkaConnectorTask> tasks) {
        this.tasks = tasks;
    }
}
