package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleetshard.api.connector.Connector;

public abstract class ConnectorEventSource<T extends Connector<?, ?>> extends DependantResourceEventSource<T> {
    protected ConnectorEventSource(KubernetesClient client) {
        super(client);
    }
}
