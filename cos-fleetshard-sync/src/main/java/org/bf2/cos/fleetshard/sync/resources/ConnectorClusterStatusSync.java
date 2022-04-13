package org.bf2.cos.fleetshard.sync.resources;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ConnectorClusterState;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Operators;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.FleetShardSyncScheduler;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ConnectorClusterStatusSync implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterStatusSync.class);
    private static final String JOB_ID = "cluster.status.sync";

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    FleetShardSyncScheduler scheduler;
    @Inject
    FleetShardSyncConfig config;
    @Inject
    MeterRegistry registry;

    private volatile MetricsRecorder recorder;

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting connector status sync");

        recorder = MetricsRecorder.of(registry, config.metrics().baseName() + "." + JOB_ID);

        scheduler.schedule(
            JOB_ID,
            ConnectorClusterStatusSyncJob.class,
            config.resources().updateInterval());
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownQuietly(JOB_ID);
    }

    public void run() {
        recorder.record(this::update);
    }

    private void update() {
        ConnectorClusterStatus status = new ConnectorClusterStatus();
        status.setPhase(ConnectorClusterState.READY);

        fleetShardClient.getOperators().stream().map(
            o -> new ConnectorClusterStatusOperators()
                .namespace(o.getMetadata().getNamespace())
                .operator(new ConnectorOperator()
                    .id(o.getMetadata().getName())
                    .type(o.getSpec().getType())
                    .version(o.getSpec().getVersion()))
                .status(Operators.PHASE_READY))
            .forEach(
                status::addOperatorsItem);

        fleetShardClient.getNamespaces().stream().map(
            n -> {
                ConnectorNamespaceState phase = ConnectorNamespaceState.DISCONNECTED;
                if (n.getStatus() != null) {
                    if (Objects.equals(Namespaces.STATUS_ACTIVE, n.getStatus().getPhase())) {
                        phase = ConnectorNamespaceState.READY;
                    } else if (Objects.equals(Namespaces.STATUS_TERMINATING, n.getStatus().getPhase())) {
                        phase = ConnectorNamespaceState.DELETING;
                    }
                }

                return new ConnectorNamespaceStatus()
                    .id(n.getMetadata().getLabels().get(Resources.LABEL_NAMESPACE_ID))
                    .version(Resources.getLabel(n, Resources.LABEL_KUBERNETES_VERSION))
                    .connectorsDeployed(fleetShardClient.getConnectors(n).size())
                    .phase(phase);
            })
            .forEach(
                status::addNamespacesItem);

        controlPlane.updateClusterStatus(status);
    }
}
