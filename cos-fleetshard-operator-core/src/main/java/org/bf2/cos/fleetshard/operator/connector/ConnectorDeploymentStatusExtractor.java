package org.bf2.cos.fleetshard.operator.connector;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatusOperators;
import org.bf2.cos.fleet.manager.model.MetaV1Condition;
import org.bf2.cos.fleetshard.api.DeployedResource;
import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.operator.client.MetaClient;
import org.bf2.cos.fleetshard.operator.connectoroperator.ConnectorOperatorSupport;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.bf2.cos.meta.model.ConnectorDeploymentStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;

@ApplicationScoped
public class ConnectorDeploymentStatusExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorDeploymentStatusExtractor.class);

    private final UnstructuredClient unstructuredClient;
    private final MetaClient metaClient;

    public ConnectorDeploymentStatusExtractor(UnstructuredClient unstructuredClient, MetaClient metaClient) {
        this.unstructuredClient = unstructuredClient;
        this.metaClient = metaClient;
    }

    protected ConnectorDeploymentStatus extract(ManagedConnector connector) {
        ConnectorDeploymentStatus ds = new ConnectorDeploymentStatus();
        DeploymentSpec deploymentSpec = connector.getSpec().getDeployment();

        if (connector.getStatus() != null && connector.getStatus().getPhase() != null) {
            deploymentSpec = connector.getStatus().getDeployment();
        }

        ds.setResourceVersion(deploymentSpec.getDeploymentResourceVersion());

        if (connector.getStatus() != null && connector.getStatus().getPhase() != null) {
            switch (connector.getStatus().getPhase()) {
                case Augmentation:
                case Initialization:
                    ds.setPhase(STATE_PROVISIONING);
                    break;
                case Deleting:
                case Stopping:
                    ds.setPhase(STATE_DE_PROVISIONING);
                    break;
                case Deleted:
                    ds.setPhase(STATE_DELETED);
                    break;
                case Stopped:
                    ds.setPhase(STATE_STOPPED);
                    break;
                case Monitor:
                    ds.setOperators(
                        new ConnectorDeploymentStatusOperators()
                            .assigned(ConnectorOperatorSupport.toConnectorOperator(connector.getStatus().getAssignedOperator()))
                            .available(
                                ConnectorOperatorSupport.toConnectorOperator(connector.getStatus().getAvailableOperator())));

                    setConnectorStatus(connector, ds);
                    break;
                default:
                    throw new IllegalStateException(
                        "Unsupported phase ("
                            + connector.getStatus().getPhase()
                            + ") for connector "
                            + connector.getMetadata().getName());
            }
        }

        if (ds.getPhase() == null) {
            switch (deploymentSpec.getDesiredState()) {
                case DESIRED_STATE_DELETED:
                case DESIRED_STATE_STOPPED:
                    ds.setPhase(STATE_DE_PROVISIONING);
                    break;
                default:
                    ds.setPhase(STATE_PROVISIONING);
            }
        }

        return ds;
    }

    private void setConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus deploymentStatus) {
        ConnectorDeploymentStatusRequest sr = new ConnectorDeploymentStatusRequest()
            .managedConnectorId(connector.getMetadata().getName())
            .deploymentId(connector.getSpec().getDeploymentId())
            .connectorId(connector.getSpec().getConnectorId())
            .connectorTypeId(connector.getSpec().getConnectorTypeId());

        for (DeployedResource resource : connector.getStatus().getResources()) {

            // don't include secrets ...
            if (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Secret", resource.getKind())) {
                continue;
            }

            sr.addResourcesItem(
                unstructuredClient.getAsNode(connector.getMetadata().getNamespace(), resource));
        }

        if (connector.getStatus().getAssignedOperator() != null && sr.getResources() != null) {
            LOGGER.debug("Send status request to meta: address={}, request={}",
                connector.getStatus().getAssignedOperator().getMetaService(),
                Serialization.asJson(sr));

            var answer = metaClient.status(
                connector.getStatus().getAssignedOperator().getMetaService(),
                sr);

            LOGGER.debug("Got status answer from meta: address={}, answer={}",
                connector.getStatus().getAssignedOperator().getMetaService(),
                Serialization.asJson(answer));

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
}
