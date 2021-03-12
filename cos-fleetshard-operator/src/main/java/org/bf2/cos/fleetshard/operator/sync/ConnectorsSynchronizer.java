package org.bf2.cos.fleetshard.operator.sync;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import org.bf2.cos.fleetshard.api.connector.Connector;
import org.bf2.cos.fleetshard.api.connector.ConnectorCluster;
import org.bf2.cos.fleetshard.api.connector.camel.CamelConnector;
import org.bf2.cos.fleetshard.api.connector.debezium.DebeziumConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConnectorsSynchronizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsSynchronizer.class);

    @Inject
    @RestClient
    ConnectorsControlPlane controlPlane;
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "cos.agent.id")
    String agentId;

    /*
     * THIS IS A POC
     *
     * to sketch how the sync task should work, if the scheduler take more time than the
     * configured interval, the task is skipped
     */
    @Scheduled(every = "{cos.agent.poll.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void pollConnectors() {
        LOGGER.debug("Polling for control plane managed connectors");

        String namespace = kubernetesClient.getNamespace();

        getConnectorCluster().ifPresent(cc -> {
            // update the ConnectorCluster state to include the new resources
            // TODO: should the control plane also be updated ? in such case it should probably a task
            //       for the ConnectorCluster controller
            kubernetesClient
                    .customResources(ConnectorCluster.class)
                    .inNamespace(namespace)
                    .createOrReplace(cc);
        });

    }

    private void pollConnectors(ConnectorCluster connectorCluster) {
        String namespace = kubernetesClient.getNamespace();

        for (Connector<?, ?> connector : getConnectors(connectorCluster)) {
            if (connector instanceof CamelConnector) {
                kubernetesClient
                        .customResources(CamelConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace((CamelConnector) connector);
            } else if (connector instanceof DebeziumConnector) {
                kubernetesClient
                        .customResources(DebeziumConnector.class)
                        .inNamespace(namespace)
                        .createOrReplace((DebeziumConnector) connector);
            } else {
                LOGGER.error("Unsupported connector type: {}", connector.getClass().getName());
                continue;
            }

            // TODO: update resource version
            connectorCluster.getStatus().setResourceVersion(connector.getSpec().getResourceVersion());
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
        if (!answer.isReady()) {
            LOGGER.debug("ConnectorCluster not yet configured");
            return Optional.empty();
        }

        return Optional.of(answer);
    }

    private List<Connector<?, ?>> getConnectors(ConnectorCluster connectorCluster) {
        List<Connector<?, ?>> connectors = controlPlane.getConnectors(
                agentId,
                connectorCluster.getStatus().getResourceVersion());

        connectors.sort(Comparator.comparingLong(c -> c.getSpec().getResourceVersion()));

        return connectors;
    }
}
