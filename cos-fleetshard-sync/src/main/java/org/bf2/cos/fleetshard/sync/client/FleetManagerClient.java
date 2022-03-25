package org.bf2.cos.fleetshard.sync.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;

import org.bf2.cos.fleet.manager.model.ConnectorClusterState;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceList;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.support.resources.Operators;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    final FleetShardSyncConfig config;
    final FleetManagerClientApi controlPlane;

    public FleetManagerClient(FleetShardSyncConfig config) {
        this.config = config;

        UriBuilder builder = UriBuilder.fromUri(config.manager().uri()).path("/api/connector_mgmt/v1");
        if (config.tenancy().enabled()) {
            builder = builder.path("/agent");
        }

        this.controlPlane = RestClientBuilder.newBuilder()
            .baseUri(builder.build())
            .register(OidcClientRequestFilter.class)
            .connectTimeout(config.manager().connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(config.manager().readTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build(FleetManagerClientApi.class);
    }

    public void getNamespaces(long gv, Consumer<Collection<ConnectorNamespace>> consumer) {
        FleetManagerClientHelper.run(() -> {
            LOGGER.debug("polling namespaces with gv: {}", gv);

            final AtomicInteger counter = new AtomicInteger();
            final List<ConnectorNamespace> items = new ArrayList<>();

            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                ConnectorNamespaceList list = controlPlane.getConnectorNamespaces(
                    config.cluster().id(),
                    Integer.toString(i),
                    null,
                    gv);

                if (list == null || list.getItems() == null || list.getItems().isEmpty()) {
                    LOGGER.info("No namespace for cluster {}", config.cluster().id());
                    break;
                }

                items.addAll(list.getItems());

                consumer.accept(items);

                if (counter.addAndGet(items.size()) == list.getTotal()) {
                    break;
                }
            }
        });
    }

    public void getDeployments(long gv, Consumer<Collection<ConnectorDeployment>> consumer) {
        FleetManagerClientHelper.run(() -> {
            LOGGER.debug("polling deployment with gv: {}", gv);

            final AtomicInteger counter = new AtomicInteger();
            final List<ConnectorDeployment> items = new ArrayList<>();

            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                ConnectorDeploymentList list = controlPlane.getConnectorDeployments(
                    config.cluster().id(),
                    Integer.toString(i),
                    null,
                    gv);

                if (list == null || list.getItems() == null || list.getItems().isEmpty()) {
                    LOGGER.info("No connectors for cluster {}", config.cluster().id());
                    break;
                }

                items.clear();
                items.addAll(list.getItems());
                items.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));

                consumer.accept(items);

                if (counter.addAndGet(items.size()) == list.getTotal()) {
                    break;
                }
            }
        });
    }

    public void updateConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus status) {
        FleetManagerClientHelper.run(() -> {
            LOGGER.info("Update connector status: cluster_id={}, deployment_id={}, status={}",
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                Serialization.asJson(status));

            controlPlane.updateConnectorDeploymentStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                status);
        });
    }

    public void updateClusterStatus(Collection<ManagedConnectorOperator> operators, Collection<Namespace> namespaces) {
        FleetManagerClientHelper.run(() -> {
            ConnectorClusterStatus status = new ConnectorClusterStatus();
            status.setPhase(ConnectorClusterState.READY);

            operators.stream().map(
                o -> new ConnectorClusterStatusOperators()
                    .namespace(o.getMetadata().getNamespace())
                    .operator(new ConnectorOperator()
                        .id(o.getMetadata().getName())
                        .type(o.getSpec().getType())
                        .version(o.getSpec().getVersion()))
                    .status(Operators.PHASE_READY))
                .forEach(
                    status::addOperatorsItem);

            if (config.tenancy().enabled()) {
                namespaces.stream().map(
                    n -> {
                        ConnectorNamespaceState phase = ConnectorNamespaceState.DISCONNECTED;
                        if (n.getStatus() != null) {
                            if (Objects.equals(Namespaces.STATUS_ACTIVE, n.getStatus().getPhase())) {
                                phase = ConnectorNamespaceState.READY;
                            } else if (Objects.equals(Namespaces.STATUS_TERMINATING, n.getStatus().getPhase())) {
                                phase = ConnectorNamespaceState.DELETING;
                            }
                        }

                        return new ConnectorNamespaceStatus()
                            .id(n.getMetadata().getLabels().get(Resources.LABEL_NAMESPACE_ID))
                            .phase(phase);
                    })
                    .forEach(
                        status::addNamespacesItem);
            }

            controlPlane.updateConnectorClusterStatus(
                config.cluster().id(),
                status);
        });
    }
}
