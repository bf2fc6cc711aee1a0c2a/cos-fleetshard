package org.bf2.cos.fleetshard.operator.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.junit.jupiter.api.Test;

@QuarkusTestResource(OperatorSetup.class)
@QuarkusTestResource(KubernetesSetup.class)
@QuarkusTest
public class ConnectorClusterTest extends TestSupport {
    @Test
    void clusterIsRegistered() {
        await(() -> {
            ManagedConnectorCluster cluster = ksrv.getClient()
                .customResources(ManagedConnectorCluster.class)
                .inNamespace(namespace)
                .withName(ConnectorClusterSupport.clusterName(clusterId))
                .get();

            return cluster != null
                && cluster.getStatus().getPhase() == ManagedConnectorClusterStatus.PhaseType.Ready;
        });

        withConnectorOperator("co-1", "type", "1.0.0", "localhost:8080");
        withConnectorOperator("co-2", "type", "1.1.0", "localhost:8080");

        await(() -> {
            var cluster = getCluster();

            return cluster != null
                && cluster.getStatus().getOperators() != null
                && cluster.getStatus().getOperators().size() == 2;
        });
    }
}
