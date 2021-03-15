package org.bf2.cos.fleetshard.api.connector.camel;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import org.bf2.cos.fleetshard.api.connector.ConnectorSpec;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CamelConnectorSpec extends ConnectorSpec {
}
