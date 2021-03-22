package org.bf2.cos.fleetshard.operator.controlplane;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleetshard.api.Agent;
import org.bf2.cos.fleetshard.api.Connector;
import org.bf2.cos.fleetshard.api.ConnectorDeployment;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ControlPlane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlane.class);

    @Inject
    @RestClient
    ControlPlaneClient controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    public void updateAgent(Agent cluster) {
        controlPlane.updateAgent(
                cluster.getSpec().getAgentId(),
                cluster.getStatus());
    }

    public List<ConnectorDeployment> getConnectors(Agent connectorAgent) {
        // TODO: check namespaces
        // TODO: check labels
        final List<Connector> connectors = kubernetesClient.customResources(Connector.class)
                .inNamespace(connectorAgent.getMetadata().getNamespace())
                .list().getItems();

        final long gv = connectors.stream()
                .mapToLong(c -> c.getSpec().getConnectorResourceVersion())
                .max()
                .orElse(0);

        return controlPlane.getConnectors(
                connectorAgent.getSpec().getAgentId(),
                gv);
    }

    public void updateConnector(String agentId, String connectorId, ConnectorDeployment.Status status) {
        controlPlane.updateConnector(agentId, connectorId, status);
    }
}
