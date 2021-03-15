package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.fabric8.kubernetes.api.model.HasMetadata;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CamelConnector.class, name = "CamelConnector"),
        @JsonSubTypes.Type(value = DebeziumConnector.class, name = "DebeziumConnector"),
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Connector<Spec extends ConnectorSpec, Status extends ConnectorStatus> extends HasMetadata {
    Spec getSpec();

    void setSpec(Spec spec);

    Status getStatus();

    void setStatus(Status status);
}
