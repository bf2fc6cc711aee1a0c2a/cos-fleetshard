package org.bf2.cos.fleetshard.operator.it;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.it.support.CamelMetaServiceSetup;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@TestProfile(ConnectorClusterTest.Profile.class)
@QuarkusTest
public class ConnectorClusterTest {
    @KubernetesTestServer
    KubernetesServer ksrv;

    @ConfigProperty(
        name = "cluster-id")
    String clusterId;

    @Test
    void clusterIsRegistered() {
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollDelay(1, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(
                () -> {
                    ManagedConnectorCluster cluster = ksrv.getClient()
                        .customResources(ManagedConnectorCluster.class)
                        .inNamespace(ksrv.getClient().getNamespace())
                        .withName(ConnectorClusterSupport.clusterName(clusterId))
                        .get();

                    return cluster != null
                        && cluster.getStatus().getPhase() == ManagedConnectorClusterStatus.PhaseType.Ready;
                });
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(OperatorSetup.class),
                new TestResourceEntry(KubernetesSetup.class),
                new TestResourceEntry(CamelMetaServiceSetup.class));
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }

}
