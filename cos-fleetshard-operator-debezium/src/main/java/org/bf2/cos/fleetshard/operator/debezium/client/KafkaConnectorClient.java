package org.bf2.cos.fleetshard.operator.debezium.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.ManagedConnector;

public interface KafkaConnectorClient {

    Optional<KafkaConnectorDetail> status(ManagedConnector connector)
        throws URISyntaxException, IOException, InterruptedException;
}
