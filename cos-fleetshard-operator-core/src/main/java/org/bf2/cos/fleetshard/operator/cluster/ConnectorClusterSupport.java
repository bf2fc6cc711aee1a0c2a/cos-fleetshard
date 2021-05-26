package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Locale;
import java.util.Optional;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;

public final class ConnectorClusterSupport {
    public static final String CLUSTER_NAME_PREFIX = "mcc-";

    private ConnectorClusterSupport() {
    }

    public static String clusterName(String clusterId) {
        String clusterName = clusterId.toLowerCase(Locale.US);
        clusterName = KubernetesResourceUtil.sanitizeName(clusterName);
        clusterName = CLUSTER_NAME_PREFIX + clusterName;

        return clusterName;
    }

    public static Optional<Operator> selectOperator(ManagedConnectorCluster cluster, OperatorSelector selector) {
        if (cluster.getStatus() == null) {
            return Optional.empty();
        }
        if (cluster.getStatus().getOperators() == null) {
            return Optional.empty();
        }

        return selector.select(cluster.getStatus().getOperators());
    }
}
