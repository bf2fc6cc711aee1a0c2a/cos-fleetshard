package org.bf2.cos.fleetshard.api.connector;

import io.fabric8.kubernetes.api.model.Namespaced;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.fabric8.kubernetes.client.CustomResource;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CamelConnector.class, name = "CamelConnector"),
        @JsonSubTypes.Type(value = DebeziumConnector.class, name = "DebeziumConnector"),
})
public class Connector<Spec, Status>
        extends CustomResource<Spec, Status>
        implements Namespaced {
}
