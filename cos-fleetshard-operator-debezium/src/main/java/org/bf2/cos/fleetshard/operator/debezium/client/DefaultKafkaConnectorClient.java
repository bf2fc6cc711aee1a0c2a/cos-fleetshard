package org.bf2.cos.fleetshard.operator.debezium.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultKafkaConnectorClient implements KafkaConnectorClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnectorClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Optional<KafkaConnectorDetail> status(ManagedConnector connector)
        throws URISyntaxException, IOException, InterruptedException {

        String uri = String.format("http://%s.%s.svc.cluster.local:8083/connectors/%s/status",
            connector.getMetadata().getName(),
            connector.getMetadata().getNamespace(),
            connector.getSpec().getConnectorId());

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();

            HttpResponse<byte[]> result = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (result.statusCode() == 200) {
                KafkaConnectorDetail status = MAPPER.readValue(result.body(), KafkaConnectorDetail.class);

                LOGGER.info("GET {} => status {}",
                    uri,
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(status));

                return Optional.of(status);
            }

        } catch (ConnectException e) {
            LOGGER.warn("Unable to connect to {}", uri, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }
}
