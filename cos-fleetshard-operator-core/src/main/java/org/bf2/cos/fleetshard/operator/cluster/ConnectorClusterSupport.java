package org.bf2.cos.fleetshard.operator.cluster;

import java.util.Locale;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

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
}
