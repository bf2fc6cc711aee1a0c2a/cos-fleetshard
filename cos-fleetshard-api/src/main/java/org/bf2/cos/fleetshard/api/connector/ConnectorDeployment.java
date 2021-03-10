package org.bf2.cos.fleetshard.api.connector;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

/**
 * The deployment configuration to connect a Kafka topic to another system.
 */
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class), editableEnabled = false)
@Version("v1alpha1")
@Group("cos.bf2.org")
public class ConnectorDeployment
        extends CustomResource<ConnectorDeploymentSpec, ConnectorDeploymentStatus>
        implements Namespaced {
}
