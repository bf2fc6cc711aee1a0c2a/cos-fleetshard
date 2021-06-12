package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleet.manager.api.model.cp.MetaV1Condition;
import org.bf2.cos.fleet.manager.api.model.meta.ConnectorDeploymentStatusRequest;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEvent;
import org.bf2.cos.fleetshard.operator.connector.ConnectorEventSource;
import org.bf2.cos.fleetshard.operator.fleet.FleetManagerClient;
import org.bf2.cos.fleetshard.operator.fleet.FleetShardClient;
import org.bf2.cos.fleetshard.operator.fleet.FleetShardMetaClient;
import org.bf2.cos.fleetshard.operator.it.support.AbstractResourceController;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSupport;
import org.bf2.cos.fleetshard.operator.it.support.UnstructuredClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(
    name = "connector-cluster",
    finalizerName = Controller.NO_FINALIZER)
public class ConnectorClusterController extends AbstractResourceController<ManagedConnectorCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorClusterController.class);

    private final TimerEventSource retryTimer;

    @Inject
    FleetManagerClient controlPlane;
    @Inject
    FleetShardClient fleetShard;
    @Inject
    UnstructuredClient uc;
    @Inject
    FleetShardMetaClient meta;

    public ConnectorClusterController() {
        this.retryTimer = new TimerEventSource();
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource(
            "_connector-operator-retry-timer",
            retryTimer);
        eventSourceManager.registerEventSource(
            "_connectors",
            new ConnectorEventSource(fleetShard));
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

        controlPlane.updateClusterStatus(cluster);

        return update
            ? UpdateControl.updateStatusSubResource(cluster)
            : UpdateControl.noUpdate();
    }

    private void handleConnectorEvent(ManagedConnector connector) {
        try {
            ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
            ds.setResourceVersion(connector.getStatus().getDeployment().getDeploymentResourceVersion());

            extractConnectorStatus(connector, ds);

            // report available operators
            ds.setOperators(
                new ConnectorDeploymentStatusOperators()
                    .assigned(OperatorSupport.toConnectorOperator(connector.getStatus().getAssignedOperator()))
                    .available(OperatorSupport.toConnectorOperator(connector.getStatus().getAvailableOperator())));

            controlPlane.updateConnectorStatus(connector, ds);
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void extractConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        ConnectorDeploymentStatusRequest sr = new ConnectorDeploymentStatusRequest()
            .managedConnectorId(connector.getMetadata().getName())
            .deploymentId(connector.getSpec().getDeploymentId())
            .connectorId(connector.getSpec().getConnectorId())
            .connectorTypeId(connector.getSpec().getConnectorTypeId());

        for (DeployedResource resource : connector.getStatus().getResources()) {
            // don't send secrets ...
            if (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Secret", resource.getKind())) {
                continue;
            }

            sr.addResourcesItem(
                uc.getAsNode(connector.getMetadata().getNamespace(), resource));
        }

        var answer = meta.status(
            connector.getStatus().getAssignedOperator().getMetaService(),
            sr);

        deploymentStatus.setPhase(answer.getPhase());

        // TODO: fix model duplications
        if (answer.getConditions() != null) {
            for (var cond : answer.getConditions()) {
                deploymentStatus.addConditionsItem(
                    new MetaV1Condition()
                        .type(cond.getType())
                        .status(cond.getStatus())
                        .message(cond.getMessage())
                        .reason(cond.getReason())
                        .lastTransitionTime(cond.getLastTransitionTime()));
            }
        }
    }
}
