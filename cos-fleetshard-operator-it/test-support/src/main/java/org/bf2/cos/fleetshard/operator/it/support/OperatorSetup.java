package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class OperatorSetup implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        return Map.of(
            "cluster-id", "c1",
            "control-plane-base-url", "http://localhost:8081");
    }

    @Override
    public void stop() {
    }
}
