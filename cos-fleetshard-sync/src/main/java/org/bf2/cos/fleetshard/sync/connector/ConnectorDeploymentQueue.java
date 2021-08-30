package org.bf2.cos.fleetshard.sync.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
    protected Collection<ConnectorDeployment> collectAll() {
        return new ArrayList<>(fleetManager.getDeployments(0));
    }

    @Override
    protected Collection<ConnectorDeployment> collectAll(Collection<Long> elements) {
        return new ArrayList<>(fleetManager.getDeployments(Collections.max(elements)));
    }
}
