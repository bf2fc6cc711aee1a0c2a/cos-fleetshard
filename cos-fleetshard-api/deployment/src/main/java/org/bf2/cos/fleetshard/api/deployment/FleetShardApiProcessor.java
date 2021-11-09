package org.bf2.cos.fleetshard.api.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class FleetShardApiProcessor {
    @BuildStep
    ReflectiveClassBuildItem fleetShardReflectiveClasses() {
        return new ReflectiveClassBuildItem(
            true,
            false,
            org.bf2.cos.fleetshard.api.ManagedConnector.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorStatus.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorSpec.class,
            org.bf2.cos.fleetshard.api.ManagedConnector.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorCluster.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorClusterSpec.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorOperator.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpec.class,
            org.bf2.cos.fleetshard.api.ManagedConnectorOperatorStatus.class);
    }
}
