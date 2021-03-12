package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CamelConnector.class, name = "CamelConnector"),
        @JsonSubTypes.Type(value = DebeziumConnector.class, name = "DebeziumConnector"),
})
public interface Connector<Spec extends ConnectorSpec, Status extends ConnectorStatus> {
    Spec getSpec();

    Status getStatus();
}
