package org.bf2.cos.fleetshard.sync.resources;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class ConnectorNamespaceProvisioner {
    public static final String DEFAULT_ADDON_PULLSECRET_NAME = "addon-pullsecret";
    public static final String TAG_NAMESPACE_ID = "id";
    public static final String TAG_NAMESPACE_REVISION = "revision";
    public static final String METRICS_SUFFIX = ".namespace.provision";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorNamespaceProvisioner.class);

    private final FleetShardClient fleetShard;
    private final FleetManagerClient fleetManager;
    private final FleetShardSyncConfig config;
    private final MetricsRecorder recorder;

    public ConnectorNamespaceProvisioner(
        FleetShardSyncConfig config,
        FleetShardClient connectorClient,
        FleetManagerClient fleetManager,
        MeterRegistry registry) {

        this.config = config;
        this.fleetShard = connectorClient;
        this.fleetManager = fleetManager;
        this.recorder = MetricsRecorder.of(registry, config.metrics().baseName() + METRICS_SUFFIX);
    }

    public void poll(long revision) {
        fleetManager.getNamespaces(
            revision,
            this::provisionNamespaces);
    }

    private void provisionNamespaces(Collection<ConnectorNamespace> namespaces) {
        for (ConnectorNamespace namespace : namespaces) {
            this.recorder.record(
                () -> provision(namespace),
                Tags.of(TAG_NAMESPACE_ID, namespace.getId()),
                e -> {
                    LOGGER.error("Failure while trying to provision connector namespace: id={}, revision={}",
                        namespace.getId(),
                        namespace.getResourceVersion(),
                        e);

                    try {
                        fleetShard.getConnectorCluster().ifPresent(cc -> {
                            fleetShard.broadcast(
                                "Warning",
                                "FailedToCreateOrUpdateResource",
                                String.format("Unable to create or update namespace %s, revision: %s, reason: %s",
                                    namespace.getId(),
                                    namespace.getResourceVersion(),
                                    e.getMessage()),
                                cc);
                        });
                    } catch (Exception ex) {
                        LOGGER.warn("Error while broadcasting events", ex);
                    }
                });
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
        LOGGER.info("Got cluster_id: {}, namespace_d: {}, state: {}",
            fleetShard.getClusterId(),
            namespace.getId(),
            namespace.getStatus().getState());

        switch (namespace.getStatus().getState()) {
            case DELETED:
            case DELETING:
                Optional<Namespace> ns = fleetShard.getNamespace(namespace.getId())
                    .filter(n -> {
                        String state = Resources.getAnnotation(n, Resources.ANNOTATION_NAMESPACE_STATE);
                        return !ConnectorNamespaceState.DELETED.getValue().equals(state)
                            && !ConnectorNamespaceState.DELETING.getValue().equals(state);
                    });

                if (ns.isEmpty()) {
                    LOGGER.info("Namespace {} is being deleted or does not exists, skip provisioning",
                        namespace.getId());
                    return;
                }
                break;
        }

        Namespace ns = new Namespace();

        KubernetesResourceUtil.getOrCreateMetadata(ns)
            .setName(fleetShard.generateNamespaceId(namespace.getId()));

        Resources.setLabels(
            ns,
            Resources.LABEL_UOW, uid(),
            Resources.LABEL_CLUSTER_ID, fleetShard.getClusterId(),
            Resources.LABEL_NAMESPACE_ID, namespace.getId(),
            Resources.LABEL_KUBERNETES_NAME, KubernetesResourceUtil.sanitizeName(namespace.getName()),
            Resources.LABEL_KUBERNETES_MANAGED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_CREATED_BY, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_PART_OF, fleetShard.getClusterId(),
            Resources.LABEL_KUBERNETES_COMPONENT, Resources.COMPONENT_NAMESPACE,
            Resources.LABEL_KUBERNETES_INSTANCE, namespace.getId(),
            Resources.LABEL_KUBERNETES_VERSION, "" + namespace.getResourceVersion(),
            Resources.LABEL_NAMESPACE_TENANT_KIND, namespace.getTenant().getKind().getValue(),
            Resources.LABEL_NAMESPACE_TENANT_ID, KubernetesResourceUtil.sanitizeName(namespace.getTenant().getId()));

        Resources.setAnnotations(
            ns,
            Resources.ANNOTATION_NAMESPACE_STATE, namespace.getStatus().getState().getValue(),
            Resources.ANNOTATION_NAMESPACE_EXPIRATION, namespace.getExpiration());

        fleetShard.createNamespace(ns);

        copyAddonPullSecret(ns);
    }
}
