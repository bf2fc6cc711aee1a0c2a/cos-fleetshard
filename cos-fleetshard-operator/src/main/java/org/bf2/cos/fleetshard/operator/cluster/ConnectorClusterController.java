package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.ConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.bf2.cos.fleetshard.operator.camel.CamelConnectorEventSource;
import org.bf2.cos.fleetshard.operator.controlplane.ControlPlane;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumConnectorEventSource;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.support.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ConnectorClusterController extends AbstractResourceController<ConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Inject
    ControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
                CamelConnectorEventSource.EVENT_SOURCE_ID,
                new CamelConnectorEventSource(kubernetesClient));
        eventSourceManager.registerEventSource(
                DebeziumConnectorEventSource.EVENT_SOURCE_ID,
                new DebeziumConnectorEventSource(kubernetesClient));
    }

    @Override
    public UpdateControl<ConnectorCluster> createOrUpdateResource(
            ConnectorCluster cluster,
            Context<ConnectorCluster> context) {

        LOGGER.info("createOrUpdateResource id={}, phase={}",
                cluster.getSpec().getConnectorClusterId(),
                cluster.getStatus().getPhase());

        if (!cluster.getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready)) {
            cluster.getStatus().setPhase(ConnectorClusterStatus.PhaseType.Ready.name());
        }
        if (cluster.getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready)) {
            // TODO: this should be made more efficient
            kubernetesClient.customResources(CamelConnector.class)
                    .inNamespace(cluster.getMetadata().getNamespace())
                    .list().getItems().forEach(con -> {
                        if (con.getSpec().getResourceVersion() > cluster.getStatus().getResourceVersion()) {
                            cluster.getStatus().setResourceVersion(con.getSpec().getResourceVersion());
                        }
                    });
            kubernetesClient.customResources(DebeziumConnector.class)
                    .inNamespace(cluster.getMetadata().getNamespace())
                    .list().getItems().forEach(con -> {
                        if (con.getSpec().getResourceVersion() > cluster.getStatus().getResourceVersion()) {
                            cluster.getStatus().setResourceVersion(con.getSpec().getResourceVersion());
                        }
                    });

        }

        controlPlane.updateConnectorCluster(cluster);

        return UpdateControl.updateStatusSubResource(cluster);
    }

    // ******************************************
    //
    // Control Plane Sync
    //
    // ******************************************

    // TODO: maybe this should be part of the control loop
    @Scheduled(every = "{cos.agent.sync.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void pollConnectors() {
        LOGGER.debug("Polling for control plane managed connectors");
        getConnectorCluster().ifPresent(this::pollConnectors);
    }

    private void pollConnectors(ConnectorCluster connectorCluster) {
        String namespace = kubernetesClient.getNamespace();
        long resourceVersion = connectorCluster.getStatus().getResourceVersion();

        for (Connector connector : controlPlane.getConnectors(connectorCluster)) {
            LOGGER.info("got {}", connector);

            if (connector instanceof CamelConnector) {
                CamelConnector cc = (CamelConnector) connector;
                ResourceUtil.setOwnerReferences(cc, connectorCluster);

                kubernetesClient
                        .customResources(CamelConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace(cc);

                // TODO: update resource version
                // TODO: check revision
                if (cc.getSpec().getResourceVersion() > resourceVersion) {
                    resourceVersion = cc.getSpec().getResourceVersion();
                }
            } else if (connector instanceof DebeziumConnector) {
                DebeziumConnector dc = (DebeziumConnector) connector;
                ResourceUtil.setOwnerReferences(dc, connectorCluster);

                kubernetesClient
                        .customResources(DebeziumConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace(dc);

                // TODO: update resource version
                // TODO: check revision
                if (dc.getSpec().getResourceVersion() > resourceVersion) {
                    resourceVersion = dc.getSpec().getResourceVersion();
                }
            } else {
                LOGGER.error("Unsupported connector type: {}", connector.getClass().getName());
            }
        }
    }

    private Optional<ConnectorCluster> getConnectorCluster() {
        String namespace = kubernetesClient.getNamespace();

        KubernetesResourceList<ConnectorCluster> items = kubernetesClient.customResources(ConnectorCluster.class)
                .inNamespace(namespace)
                .list();

        if (items.getItems().isEmpty()) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }
        if (items.getItems().size() > 1) {
            // TODO: report the failure status to the CR and control plane
            throw new IllegalStateException("TODO");
        }

        ConnectorCluster answer = items.getItems().get(0);
        if (!answer.getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready)) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }

        return Optional.of(answer);
    }
}
