package org.bf2.cos.fleetshard.api.connector.debezium;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import org.bf2.cos.fleetshard.api.connector.Connector;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class), editableEnabled = false)
@Version(DebeziumConnector.VERSION)
@Group(DebeziumConnector.GROUP)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DebeziumConnector
        extends CustomResource<DebeziumConnectorSpec, DebeziumConnectorStatus>
        implements Connector<DebeziumConnectorSpec, DebeziumConnectorStatus> {

    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "cos.bf2.org";
    public static final String KIND = "DebeziumConnector";
}