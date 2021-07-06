package org.bf2.cos.fleetshard.operator.it.support.camel;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class CamelMetaServiceSetup implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelMetaServiceSetup.class);

    //"quay.io/lburgazzoli/cms:native";
    private static final String DEFAULT_META_IMAGE = "quay.io/rhoas/cos-fleetshard-meta-camel:latest";
    private static final int META_PORT = 8080;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            String meta = System.getenv("COS_FLEETSHARD_META_IMAGE");
            if (meta == null) {
                meta = DEFAULT_META_IMAGE;
            }

            LOGGER.info("Starting meta {}", meta);

            container = new GenericContainer<>(meta)
                .withEnv("QUARKUS_LOG_CONSOLE_JSON", "false")
                .withExposedPorts(META_PORT)
                //.withLogConsumer(new Slf4jLogConsumer(LOGGER))
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