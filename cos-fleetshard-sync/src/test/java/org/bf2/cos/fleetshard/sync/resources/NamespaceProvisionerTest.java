package org.bf2.cos.fleetshard.sync.resources;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.connector.ConnectorTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;

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
        final ConnectorNamespaceDeployment namespace = new ConnectorNamespaceDeployment();

        namespace.id(uid());
        namespace.name("--eval");

        ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
            .id(uid())
            .kind(ConnectorNamespaceTenantKind.ORGANISATION);

        namespace.setStatus(new ConnectorNamespaceStatus().state(ConnectorNamespaceState.READY).connectorsDeployed(0));
        namespace.setTenant(tenant);
        namespace.setExpiration(new Date().toString());

        final List<ManagedConnector> connectors = List.of();
        final List<Secret> secrets = List.of();

        final ConnectorNamespaceProvisioner provisioner = new ConnectorNamespaceProvisioner();
        provisioner.config = ConnectorTestSupport.config();
        ;
        provisioner.fleetShard = ConnectorTestSupport.fleetShard(CLUSTER_ID, connectors, secrets);
        provisioner.fleetManager = ConnectorTestSupport.fleetManagerClient();
        provisioner.eventClient = Mockito.mock(EventClient.class);
        provisioner.recorder = Mockito.mock(MetricsRecorder.class);

        final ArgumentCaptor<Namespace> nc = ArgumentCaptor.forClass(Namespace.class);

        //
        // When deployment is applied
        //
        provisioner.provision(namespace);

        verify(provisioner.fleetShard).createNamespace(nc.capture());

        //
        // Then resources must be created according to the deployment
        //
        assertThat(nc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_KUBERNETES_NAME, "a--eval");

        });
    }
}
