package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class CamelMetaServiceSetup implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelMetaServiceSetup.class);

    private static final String META_IMAGE = "quay.io/lburgazzoli/cms:latest";
    private static final int META_PORT = 8080;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            LOGGER.info("Starting meta {}", META_IMAGE);

            container = new GenericContainer<>(META_IMAGE)
                .withEnv("QUARKUS_LOG_CONSOLE_JSON", "false")
                .withExposedPorts(META_PORT)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forListeningPort());

            container.start();

            return Collections.singletonMap(
                "cos.fleetshard.meta.camel",
                String.format("%s:%d", container.getContainerIpAddress(), container.getMappedPort(META_PORT)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}