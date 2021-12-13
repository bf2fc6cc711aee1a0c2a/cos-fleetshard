package org.bf2.cos.fleetshard.sync.connector;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleetshard.support.EventQueue;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;

@ApplicationScoped
public class ConnectorDeploymentQueue extends EventQueue<Long, ConnectorDeployment> {
    @Inject
    FleetManagerClient fleetManager;

    public ConnectorDeploymentQueue() {
    }

    @Override
    protected void process(Collection<Long> elements, Consumer<Collection<ConnectorDeployment>> consumer) {
        final long revision = elements.isEmpty()
            ? 0
            : Collections.max(elements);

        fleetManager.getDeployments(revision, consumer);
    }
}
