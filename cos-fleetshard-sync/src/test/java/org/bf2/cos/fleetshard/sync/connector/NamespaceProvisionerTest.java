package org.bf2.cos.fleetshard.sync.connector;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus1;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.micrometer.core.instrument.MeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_KUBERNETES_NAME;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;
import static org.mockito.Mockito.verify;

public class NamespaceProvisionerTest {
    private static final String CLUSTER_ID = UUID.randomUUID().toString();

    @Test
    void nameIsSanitized() {
        //
        // Given that no resources associated to the provided deployment exist
        //
        final ConnectorNamespace namespace = new ConnectorNamespace();

        namespace.id(uid());
        namespace.name("--eval");

        ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
            .id(uid())
            .kind(ConnectorNamespaceTenantKind.ORGANISATION);

        namespace.setStatus(new ConnectorNamespaceStatus1().state(ConnectorNamespaceState.READY).connectorsDeployed(0));
        namespace.setTenant(tenant);
        namespace.setExpiration(new Date().toString());

        final List<ManagedConnector> connectors = List.of();
        final List<Secret> secrets = List.of();

        final FleetShardClient fleetShard = ConnectorTestSupport.fleetShard(CLUSTER_ID, connectors, secrets);
        final FleetManagerClient fleetManager = ConnectorTestSupport.fleetManagerClient();
        final FleetShardSyncConfig config = ConnectorTestSupport.config();
        final MeterRegistry registry = Mockito.mock(MeterRegistry.class);
        final ConnectorNamespaceProvisioner provisioner = new ConnectorNamespaceProvisioner(config, fleetShard, fleetManager,
            registry);
        final ArgumentCaptor<Namespace> nc = ArgumentCaptor.forClass(Namespace.class);

        //
        // When deployment is applied
        //
        provisioner.provision(namespace);

        verify(fleetShard).createNamespace(nc.capture());

        //
        // Then resources must be created according to the deployment
        //
        assertThat(nc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_KUBERNETES_NAME, "a--eval");

        });
    }
}
