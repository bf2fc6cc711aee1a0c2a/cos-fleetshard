package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class OperatorSetup implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorSetup.class);

    @Override
    public Map<String, String> start() {
        var properties = Map.of(
            "cluster-id", "c1",
            "control-plane-base-url", "http://localhost:8081",
            "mas-sso-base-url", "http://localhost:8001",
            "mas-sso-realm", "mas",
            "client-id", UUID.randomUUID().toString(),
            "client-secret", UUID.randomUUID().toString(),
            "test.namespace", "test");

        LOGGER.info("Configuring Operator properties: {}", properties);

        return properties;
    }

    @Override
    public void stop() {
    }
}
