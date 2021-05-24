package org.bf2.cos.fleetshard.operator.fleet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;

@ApplicationScoped
public class FleetShard {
    @Inject
    KubernetesClient kubernetesClient;

    public ManagedConnectorCluster lookupManagedConnectorCluster(String namespace) {
        var items = kubernetesClient.customResources(ManagedConnectorCluster.class)
            .inNamespace(namespace)
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors clusters");
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }

    public ManagedConnector lookupManagedConnector(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        var items = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorCluster.getSpec().getConnectorsNamespace())
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, deployment.getId())
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple connectors with id " + deployment.getSpec().getConnectorId());
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }

    public Service lookupMetaService(ManagedConnectorCluster connectorCluster, ConnectorDeployment deployment) {
        var image = deployment.getSpec().getShardMetadata().requiredAt("/meta_image").asText();
        image = KubernetesResourceUtil.sanitizeName(image);

        var items = kubernetesClient.services()
            .inNamespace(connectorCluster.getSpec().getConnectorsNamespace())
            .withLabel(ManagedConnector.LABEL_CONNECTOR_META, "true")
            .withLabel(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, image)
            .list();

        if (items.getItems() != null && items.getItems().size() > 1) {
            throw new IllegalArgumentException(
                "Multiple meta service for image " + image);
        }
        if (items.getItems() != null && items.getItems().size() == 1) {
            return items.getItems().get(0);
        }

        return null;
    }
}
