package org.bf2.cos.fleetshard.operator.client;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.meta.api.ConnectorMetaServiceApi;
import org.bf2.cos.meta.model.ConnectorDeploymentReifyRequest;
import org.bf2.cos.meta.model.ConnectorDeploymentSpec;
import org.bf2.cos.meta.model.ConnectorDeploymentStatus;
import org.bf2.cos.meta.model.ConnectorDeploymentStatusRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import static org.bf2.cos.fleetshard.operator.client.MetaClientHelper.call;

@ApplicationScoped
public class MetaClient {
    private final ConcurrentMap<String, ConnectorMetaServiceApi> clients;

    @ConfigProperty(
        name = "cos.meta.connect.timeout",
        defaultValue = "5s")
    Duration connectTimeout;

    @ConfigProperty(
        name = "cos.meta.read.timeout",
        defaultValue = "10s")
    Duration readTimeout;

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
        String uri = address;
        if (!uri.startsWith("http://")) {
            uri = "http://" + uri;
        }

        return this.clients.computeIfAbsent(
            uri,
            a -> RestClientBuilder.newBuilder()
                .baseUri(URI.create(a))
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build(ConnectorMetaServiceApi.class));
    }
}
