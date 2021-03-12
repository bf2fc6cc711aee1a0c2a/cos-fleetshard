package org.bf2.cos.fleetshard.api.connector.debezium;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import org.bf2.cos.fleetshard.api.connector.Connector;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class))
@Version("v1alpha1")
@Group("cos.bf2.org")
public class DebeziumConnector
        extends CustomResource<DebeziumConnectorSpec, DebeziumConnectorStatus>
        implements Namespaced, Connector<DebeziumConnectorSpec, DebeziumConnectorStatus> {
}