package org.bf2.cos.fleetshard.operator.it.debezium.support;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorClient;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetail;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class DebeziumTestClient implements KafkaConnectorClient {
    @Inject
    DebeziumConnectContext connectContext;

    @Override
    public Optional<KafkaConnectorDetail> status(ManagedConnector connector)
        throws URISyntaxException, IOException, InterruptedException {

        return Optional.ofNullable(connectContext.getDetail());
    }
}
