package org.bf2.cos.fleetshard.operator.client;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentReifyRequest;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentSpec;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class MetaClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaClient.class);

    private final Vertx vertx;

    public MetaClient(io.vertx.core.Vertx vertx) {
        this.vertx = new Vertx(vertx);
    }

    public ConnectorDeploymentSpec reify(String address, ConnectorDeploymentReifyRequest request) {
        return send(address, "/reify", request, ConnectorDeploymentSpec.class);
    }

    public ConnectorDeploymentStatus status(String address, ConnectorDeploymentStatusRequest request) {
        return send(address, "/status", request, ConnectorDeploymentStatus.class);
    }

    public <T> T send(String address, String uri, Object payload, Class<T> responseType) {
        WebClient client = WebClient.create(vertx);

        try {
            final String[] hp = address.split(":");
            final String host = hp[0];
            final int port = hp.length == 2 ? Integer.parseInt(hp[1]) : 80;

            LOGGER.debug("Send request to meta: address={}, uri={}, request={}",
                address,
                uri,
                Serialization.asJson(payload));

            // TODO: set-up ssl/tls
            // TODO: make timeout configurable
            var answer = client.post(port, host, uri)
                .sendJson(payload)
                .await()
                .atMost(Duration.ofSeconds(30))
                .bodyAsJson(responseType);

            LOGGER.debug("Got answer from meta: address={}, uri={}, answer={}",
                address,
                uri,
                Serialization.asJson(answer));

            return answer;
        } catch (Exception e) {
            throw new MetaClientException(e);
        } finally {
            client.close();
        }
    }
}
