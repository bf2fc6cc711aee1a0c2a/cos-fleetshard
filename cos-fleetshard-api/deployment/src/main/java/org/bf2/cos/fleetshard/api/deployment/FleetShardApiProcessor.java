package org.bf2.cos.fleetshard.api.deployment;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

public class FleetShardApiProcessor {
    @BuildStep
    List<ReflectiveHierarchyBuildItem> fleetShardReflectiveClasses() {
        return List.of(
            reflectiveHierarchy(org.bf2.cos.fleetshard.api.ManagedConnector.class),
            reflectiveHierarchy(org.bf2.cos.fleetshard.api.ManagedConnectorCluster.class),
            reflectiveHierarchy(org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpec.class));
    }

    private ReflectiveHierarchyBuildItem reflectiveHierarchy(Class<?> type) {
        final var className = DotName.createSimple(type.getName());
        final var classType = Type.create(className, Type.Kind.CLASS);

        return new ReflectiveHierarchyBuildItem.Builder()
            .type(classType)
            .build();
    }
}
