package org.bf2.cos.fleetshard.it;

import java.util.Map;
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MetaServiceTestResource
    implements QuarkusTestResourceConfigurableLifecycleManager<WithMetaServiceTestResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaServiceTestResource.class);

    private String image;
    private String prefix;
    private int port;
    private GenericContainer<?> container;

    @Override
    public void init(WithMetaServiceTestResource annotation) {
        this.image = annotation.image();
        this.prefix = annotation.prefix();
        this.port = annotation.port();
    }

    @Override
    public void init(Map<String, String> initArgs) {
        if (!initArgs.isEmpty()) {
            this.image = Objects.requireNonNull(initArgs.get("image"));
            this.prefix = Objects.requireNonNull(initArgs.get("prefix"));
            this.port = Integer.parseInt(initArgs.getOrDefault("port", "8080"));
        }
    }

    @Override
    public Map<String, String> start() {
        try {
            LOGGER.info("Starting meta image: {}, port: {}, prefix: {}", image, port, prefix);

            container = new GenericContainer<>(image)
                .withEnv("QUARKUS_LOG_CONSOLE_JSON", "false")
                .withExposedPorts(port)
                .waitingFor(Wait.forListeningPort());

            container.start();

            return Map.of(
                prefix + ".url",
                String.format("%s:%d", container.getContainerIpAddress(), container.getMappedPort(port)));
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