package org.bf2.cos.fleetshard.operator.it.support;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesSetup extends KubernetesServerTestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesSetup.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern(VersionInfo.VersionKeys.BUILD_DATE_FORMAT);

    @Override
    public void configureServer() {
        LOGGER.info("Configuring KubernetesServer");

        try {
            server.expect().get().withPath("/version")
                .andReturn(200, new VersionInfo.Builder()
                    .withBuildDate(DATE_FORMATTER.format(LocalDateTime.now()))
                    .withMajor("1")
                    .withMinor("21")
                    .build())
                .always();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
