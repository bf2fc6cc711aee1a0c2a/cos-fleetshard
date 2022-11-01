package org.bf2.cos.fleetshard.operator.service.client;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;

import org.bf2.cos.fleet.manager.client.AuthRequestFilter;
import org.bf2.cos.fleet.manager.client.ClientConfig;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@ApplicationScoped
public class ServiceClient {
    final ClientConfig config;
    final ServiceApi controlPlane;

    public ServiceClient(ClientConfig config, AuthRequestFilter filter) {
        this.config = config;

        // TODO: new endpoint
        UriBuilder builder = UriBuilder.fromUri(config.manager().uri())
            .path("/api/connector_mgmt/v1/agent");

        this.controlPlane = RestClientBuilder.newBuilder()
            .baseUri(builder.build())
            .register(filter)
            .connectTimeout(config.manager().connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(config.manager().readTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build(ServiceApi.class);
    }
}
