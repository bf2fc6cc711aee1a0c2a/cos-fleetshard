package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CamelConnector.class, name = "CamelConnector"),
        @JsonSubTypes.Type(value = DebeziumConnector.class, name = "DebeziumConnector"),
})
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class))
public abstract class Connector<Spec extends ConnectorSpec, Status extends ConnectorStatus>
        extends CustomResource<Spec, Status>
        implements Namespaced {
}
