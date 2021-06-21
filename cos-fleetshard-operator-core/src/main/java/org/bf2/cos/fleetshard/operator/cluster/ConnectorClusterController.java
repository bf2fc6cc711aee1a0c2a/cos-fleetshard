package org.bf2.cos.fleetshard.operator.cluster;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.client.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEvent;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEventSource;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorEventSource;
import org.bf2.cos.fleetshard.operator.support.AbstractResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(
    name = "connector-cluster",
    finalizerName = Controller.NO_FINALIZER)
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;

    @Override
    public void registerEventSources(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_connectors",
            new ConnectorEventSource(kubernetesClient, fleetShard.getConnectorsNamespace()) {
                @Override
                protected void resourceUpdated(ManagedConnector resource) {
                    eventHandler.handleEvent(new ConnectorEvent(
                        resource.getMetadata().getOwnerReferences().get(0).getUid(),
                        this,
                        resource.getMetadata().getName(),
                        resource.getMetadata().getNamespace()));
                }
            });
        eventSourceManager.registerEventSource(
            "_operators",
            new ConnectorOperatorEventSource(kubernetesClient) {
                @Override
                protected void resourceUpdated(ManagedConnectorOperator resource) {
                    // TODO
                }
            });
    }

    @Override
    public UpdateControl<ManagedConnectorCluster> createOrUpdateResource(
        ManagedConnectorCluster cluster,
        Context<ManagedConnectorCluster> context) {

        for (Event event : context.getEvents().getList()) {
            if (event instanceof ConnectorEvent) {
                final String ns = ((ConnectorEvent) event).getConnectorNamespace();
                final String name = ((ConnectorEvent) event).getConnectorName();

                LOGGER.info("got event on  {}/{}", ns, name);

                fleetShard.lookupManagedConnector(ns, name).ifPresentOrElse(
                    this::handleConnectorEvent,
                    () -> LOGGER.info("Unable to find connector {}/{}", ns, name));
            }
        }

        boolean update = false;
        if (!cluster.getStatus().isReady()) {
            cluster.getStatus().setPhase(ManagedConnectorClusterStatus.PhaseType.Ready);
            update = true;
        }

        controlPlane.updateClusterStatus(
            cluster,
            fleetShard.lookupManagedConnectorOperators());

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }

    // **************************************************
    //
    // Connectors
    //
    // **************************************************

    private void handleConnectorEvent(ManagedConnector connector) {
        try {
            controlPlane.updateConnectorStatus(connector,
                fleetShard.getConnectorDeploymentStatus(connector));
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
