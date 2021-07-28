package org.bf2.cos.fleetshard.operator.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.operator.cluster.ConnectorClusterSupport;
import org.bf2.cos.fleetshard.operator.it.support.OidcSetup;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;
import org.bf2.cos.fleetshard.operator.it.support.TestSupport;
import org.junit.jupiter.api.Test;

import static org.bf2.cos.fleetshard.operator.it.support.assertions.Assertions.assertThat;

@QuarkusTestResource(OidcSetup.class)
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

            assertThat(cluster)
                .isNotNull();
            assertThat(cluster.getStatus().getPhase())
                .isEqualTo(ManagedConnectorClusterStatus.PhaseType.Unconnected);
        });

        withConnectorOperator("co-1", "type", "1.0.0", "localhost:8080");
        withConnectorOperator("co-2", "type", "1.1.0", "localhost:8080");

        await(() -> {
            ManagedConnectorCluster cluster = ksrv.getClient()
                .customResources(ManagedConnectorCluster.class)
                .inNamespace(namespace)
                .withName(ConnectorClusterSupport.clusterName(clusterId))
                .get();

            assertThat(cluster)
                .isNotNull();
            assertThat(cluster.getStatus().getPhase())
                .isEqualTo(ManagedConnectorClusterStatus.PhaseType.Ready);
        });

        await(() -> {
            var cluster = getCluster();

            assertThat(cluster)
                .isNotNull();
            assertThat(cluster.getStatus().getOperators())
                .hasSize(2);
        });
    }
}
