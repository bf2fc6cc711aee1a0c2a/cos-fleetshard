package org.bf2.cos.fleetshard.api;

import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ConnectorList
        extends CustomResourceList<Connector> {
}
