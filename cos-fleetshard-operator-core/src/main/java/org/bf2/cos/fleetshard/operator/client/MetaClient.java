package org.bf2.cos.fleetshard.operator.client;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.meta.api.ConnectorMetaServiceApi;
import org.bf2.cos.meta.model.ConnectorDeploymentReifyRequest;
import org.bf2.cos.meta.model.ConnectorDeploymentSpec;
import org.bf2.cos.meta.model.ConnectorDeploymentStatus;
import org.bf2.cos.meta.model.ConnectorDeploymentStatusRequest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import static org.bf2.cos.fleetshard.operator.client.MetaClientHelper.call;

@ApplicationScoped
public class MetaClient {
    private final ConcurrentMap<String, ConnectorMetaServiceApi> clients;

    public MetaClient() {
        this.clients = new ConcurrentHashMap<>();
    }

    public ConnectorDeploymentSpec reify(String address, ConnectorDeploymentReifyRequest request) {
        return call(() -> client(address).connectorDeploymentReifyRequest(request));
    }

    public ConnectorDeploymentStatus status(String address, ConnectorDeploymentStatusRequest request) {
        return call(() -> client(address).connectorDeploymentStatusRequest(request));
    }

    private ConnectorMetaServiceApi client(String address) {
        // TODO: set-up ssl/tls
        // TODO: make timeout configurable
        String uri = address;
        if (!uri.startsWith("http://")) {
            uri = "http://" + uri;
        }

        return this.clients.computeIfAbsent(
            uri,
            a -> RestClientBuilder.newBuilder()
                .baseUri(URI.create(a))
                .build(ConnectorMetaServiceApi.class));
    }
}
