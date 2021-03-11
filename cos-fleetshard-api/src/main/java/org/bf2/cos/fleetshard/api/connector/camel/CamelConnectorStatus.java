package org.bf2.cos.fleetshard.api.connector.camel;

import io.sundr.builder.annotations.Buildable;
import org.bf2.cos.fleetshard.api.connector.ConnectorStatus;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class CamelConnectorStatus extends ConnectorStatus {
}
