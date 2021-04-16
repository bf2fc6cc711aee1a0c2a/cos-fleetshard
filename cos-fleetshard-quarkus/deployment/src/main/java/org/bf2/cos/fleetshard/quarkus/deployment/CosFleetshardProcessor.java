package org.bf2.cos.fleetshard.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class CosFleetshardProcessor {
    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(
            "java.io.Closeable",
            "java.lang.AutoCloseable");
    }
}
