package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.EventQueue;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

@ApplicationScoped
class ConnectorStatusQueue extends EventQueue<String, ManagedConnector> {
    @Inject
    FleetShardClient connectorClient;

    @Override
    protected Collection<ManagedConnector> collectAll() {
        return connectorClient.getAllConnectors();
    }

    @Override
    protected Collection<ManagedConnector> collectAll(Collection<String> elements) {
        return elements.stream()
            .sorted()
            .distinct()
            .flatMap(e -> connectorClient.getConnectorByName(e).stream())
            .collect(Collectors.toList());
    }
}
