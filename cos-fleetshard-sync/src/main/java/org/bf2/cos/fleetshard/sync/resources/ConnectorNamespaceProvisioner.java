package org.bf2.cos.fleetshard.sync.resources;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

@ApplicationScoped
public class ConnectorNamespaceProvisioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorNamespaceProvisioner.class);

    private final FleetShardClient fleetShard;
    private final FleetManagerClient fleetManager;
    private final FleetShardSyncConfig config;

    public ConnectorNamespaceProvisioner(
        FleetShardSyncConfig config,
        FleetShardClient connectorClient,
        FleetManagerClient fleetManager) {

        this.config = config;
        this.fleetShard = connectorClient;
        this.fleetManager = fleetManager;
    }

    public void poll(long revision) {
        if (!config.tenancy().enabled()) {
            return;
        }

        fleetManager.getNamespaces(
            revision,
            this::provisionNamespaces);
    }

    private void provisionNamespaces(Collection<ConnectorNamespace> namespaces) {
        LOGGER.debug("namespaces: {}", namespaces.size());

        for (ConnectorNamespace namespace : namespaces) {
            provision(namespace);
        }
    }

    public void provision(ConnectorNamespace namespace) {
        LOGGER.info("Got cluster_id: {}, namespace_d: {}",
            fleetShard.getClusterId(),
            namespace.getId());

        Namespace ns = new Namespace();

        KubernetesResourceUtil.getOrCreateMetadata(ns)
            .setName(Namespaces.generateNamespaceId(namespace.getId()));

        Resources.setLabels(
            ns,
            Resources.LABEL_CLUSTER_ID, fleetShard.getClusterId(),
            Resources.LABEL_NAMESPACE_ID, namespace.getId(),
            Resources.LABEL_KUBERNETES_NAME, namespace.getName(),
            Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_CREATED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_PART_OF, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE,
            Resources.LABEL_KUBERNETES_INSTANCE, namespace.getId());

        Resources.setAnnotations(
            ns,
            Resources.ANNOTATION_NAMESPACE_EXPIRATION, namespace.getExpiration(),
            Resources.ANNOTATION_NAMESPACE_TENAT_KIND, namespace.getTenant().getKind().toString(),
            Resources.ANNOTATION_NAMESPACE_TENAT_ID, namespace.getTenant().getId(),
            Resources.ANNOTATION_NAMESPACE_RESOURCE_VERSION, "" + namespace.getResourceVersion());

        fleetShard.getKubernetesClient().namespaces().createOrReplace(ns);
    }
}
