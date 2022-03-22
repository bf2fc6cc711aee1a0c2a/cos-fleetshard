package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.EventQueue;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;

@ApplicationScoped
class ConnectorStatusQueue extends EventQueue<NamespacedName, ManagedConnector> {
    @Inject
    FleetShardClient connectorClient;

    @Override
    protected void process(Collection<NamespacedName> elements, Consumer<Collection<ManagedConnector>> consumer) {
        final Collection<ManagedConnector> connectors = elements.isEmpty()
            ? connectorClient.getAllConnectors()
            : elements.stream()
                .sorted()
                .distinct()
                .flatMap(e -> connectorClient.getConnector(e).stream())
                .collect(Collectors.toList());

        consumer.accept(connectors);

    }
}
