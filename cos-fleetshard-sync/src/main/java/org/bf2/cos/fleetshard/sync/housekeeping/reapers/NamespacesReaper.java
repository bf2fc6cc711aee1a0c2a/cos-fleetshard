package org.bf2.cos.fleetshard.sync.housekeeping.reapers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleetshard.support.Service;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.watch.Informers;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.housekeeping.Housekeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;

@ApplicationScoped
public class NamespacesReaper implements Housekeeper.Task, Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespacesReaper.class);

    public static final String ID = "namespace.reaper";

    private final FleetShardClient fleetShardClient;
    private final AtomicBoolean running;
    private final AtomicBoolean taskRunning;

    public NamespacesReaper(FleetShardClient fleetShardClient) {
        this.fleetShardClient = fleetShardClient;
        this.running = new AtomicBoolean();
        this.taskRunning = new AtomicBoolean();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void start() throws Exception {
        this.running.set(true);
        this.fleetShardClient.watchNamespaces(Informers.wrap(ns -> run()));
    }

    @Override
    public void stop() throws Exception {
        this.running.set(false);
    }

    @Override
    public void run() {
        if (running.get() && taskRunning.compareAndSet(false, true)) {
            try {
                doRun();
            } finally {
                taskRunning.set(false);
            }
        }
    }

    private void doRun() {
        for (Namespace ns : fleetShardClient.getNamespaces()) {
            String annotation = Resources.getAnnotation(ns, Resources.ANNOTATION_NAMESPACE_STATE);
            if (annotation == null) {
                continue;
            }

            if (!Objects.equals(ConnectorNamespaceState.DELETING.getValue(), annotation) &&
                !Objects.equals(ConnectorNamespaceState.DELETED.getValue(), annotation)) {
                continue;
            }

            String connectors = Resources.getAnnotation(ns, Resources.ANNOTATION_NAMESPACE_CONNECTORS);
            if (!"0".equals(connectors)) {
                continue;
            }

            if (fleetShardClient.getConnectors(ns).isEmpty()) {
                try {
                    LOGGER.info("Deleting namespace: {} (id: {}, state: {}, expiration: {})",
                        ns.getMetadata().getName(),
                        Resources.getLabel(ns, Resources.LABEL_NAMESPACE_ID),
                        Resources.getAnnotation(ns, Resources.ANNOTATION_NAMESPACE_STATE),
                        Resources.getAnnotation(ns, Resources.ANNOTATION_NAMESPACE_EXPIRATION));

                    fleetShardClient.deleteNamespace(ns);
                } catch (Exception e) {
                    LOGGER.debug("Failure deleting namespace {}", ns.getMetadata().getName());
                }
            }
        }
    }
}
