package org.bf2.cos.fleetshard.operator.it;

import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTest
public class ConnectorClusterTest {
    @KubernetesTestServer
    KubernetesServer ksrv;

    @ConfigProperty(
        name = "cluster-id")
    String clusterId;

    @Test
    void clusterIsRegistered() {
        TestSupport.await(30, TimeUnit.SECONDS, () -> {
            ManagedConnectorCluster cluster = ksrv.getClient()
                .customResources(ManagedConnectorCluster.class)
                .inNamespace(ksrv.getClient().getNamespace())
                .withName(ConnectorClusterSupport.clusterName(clusterId))
                .get();

            return cluster != null
                && cluster.getStatus().getPhase() == ManagedConnectorClusterStatus.PhaseType.Ready;
        });
    }
}
