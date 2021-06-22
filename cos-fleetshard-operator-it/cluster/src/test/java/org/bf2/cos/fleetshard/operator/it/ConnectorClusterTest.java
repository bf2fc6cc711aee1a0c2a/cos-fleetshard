package org.bf2.cos.fleetshard.operator.it;

import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTest
public class ConnectorClusterTest extends TestSupport {
    @KubernetesTestServer
    KubernetesServer ksrv;

    @ConfigProperty(
        name = "cluster-id")
    String clusterId;
    @ConfigProperty(
        name = "test.namespace")
    String namespace;

    @Test
    void clusterIsRegistered() {
        await(30, TimeUnit.SECONDS, () -> {
            ManagedConnectorCluster cluster = ksrv.getClient()
                .customResources(ManagedConnectorCluster.class)
                .inNamespace(namespace)
                .withName(ConnectorClusterSupport.clusterName(clusterId))
                .get();

            return cluster != null
                && cluster.getStatus().getPhase() == ManagedConnectorClusterStatus.PhaseType.Ready;
        });

        ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .createOrReplace(
                newConnectorOperator(namespace, "cm-1", "1.0.0", "localhost:8080"));
        ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .createOrReplace(
                newConnectorOperator(namespace, "cm-2", "1.1.0", "localhost:8080"));

        await(30, TimeUnit.SECONDS, () -> {
            var cluster = getCluster();

            return cluster != null
                && cluster.getStatus().getOperators() != null
                && cluster.getStatus().getOperators().size() == 2;
        });
    }
}
