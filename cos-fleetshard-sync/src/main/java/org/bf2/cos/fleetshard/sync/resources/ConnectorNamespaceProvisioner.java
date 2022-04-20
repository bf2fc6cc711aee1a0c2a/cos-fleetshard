package org.bf2.cos.fleetshard.sync.resources;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

@ApplicationScoped
public class ConnectorNamespaceProvisioner {
    public static final String DEFAULT_ADDON_PULLSECRET_NAME = "addon-pullsecret";

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
        fleetManager.getNamespaces(
            revision,
            this::provisionNamespaces);
    }

    private void provisionNamespaces(Collection<ConnectorNamespace> namespaces) {
        LOGGER.debug("namespaces: {}", namespaces.size());

        for (ConnectorNamespace namespace : namespaces) {
            try {
                provision(namespace);
            } catch (Exception e) {
                LOGGER.error("Failure while trying to provision namespace: {}", namespace);
            }
        }
    }

    private void copyAddonPullSecret(Namespace namespace) {
        var named = new NamespacedName(config.namespace(), config.imagePullSecretsName());

        fleetShard.getSecret(named).ifPresentOrElse(
            addonPullSecret -> {
                Secret tenantPullSecret = new Secret();

                ObjectMeta addonPullSecretMetadata = new ObjectMeta();
                addonPullSecretMetadata.setNamespace(namespace.getMetadata().getName());
                addonPullSecretMetadata.setName(addonPullSecret.getMetadata().getName());

                tenantPullSecret.setMetadata(addonPullSecretMetadata);
                tenantPullSecret.setType(addonPullSecret.getType());
                tenantPullSecret.setData(addonPullSecret.getData());

                fleetShard.createSecret(tenantPullSecret);
            },
            () -> {
                LOGGER.warn("Pull Secret {} does not exists", named);
            });
    }

    public void provision(ConnectorNamespace namespace) {
        LOGGER.info("Got cluster_id: {}, namespace_d: {}",
            fleetShard.getClusterId(),
            namespace.getId());

        Namespace ns = new Namespace();

        KubernetesResourceUtil.getOrCreateMetadata(ns)
            .setName(fleetShard.generateNamespaceId(namespace.getId()));

        Resources.setLabels(
            ns,
            Resources.LABEL_CLUSTER_ID, fleetShard.getClusterId(),
            Resources.LABEL_NAMESPACE_ID, namespace.getId(),
            Resources.LABEL_KUBERNETES_NAME, namespace.getName(),
            Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_CREATED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_PART_OF, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE,
            Resources.LABEL_KUBERNETES_INSTANCE, namespace.getId(),
            Resources.LABEL_KUBERNETES_VERSION, "" + namespace.getResourceVersion(),
            Resources.LABEL_NAMESPACE_TENANT_KIND, namespace.getTenant().getKind().getValue(),
            Resources.LABEL_NAMESPACE_TENANT_ID, namespace.getTenant().getId());

        Resources.setAnnotations(
            ns,
            Resources.ANNOTATION_NAMESPACE_STATE, namespace.getStatus().getState().getValue(),
            Resources.ANNOTATION_NAMESPACE_EXPIRATION, namespace.getExpiration());

        fleetShard.getKubernetesClient().namespaces().createOrReplace(ns);

        copyAddonPullSecret(ns);
    }
}
