package org.bf2.cos.fleetshard.support.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.PropertiesConfigSource;

public class ApplicationOverrideConfigSourceProvider implements ConfigSourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationOverrideConfigSourceProvider.class);
    private static final String OVERRIDE_PROPERTIES_LOCATION = "OVERRIDE_PROPERTIES_LOCATION";

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        String overridePropertiesLocation = System.getenv(OVERRIDE_PROPERTIES_LOCATION);
        if (overridePropertiesLocation == null) {
            LOGGER.info("Properties Override support is disabled since OVERRIDE_PROPERTIES_LOCATION env var was not set.");
            return Collections.emptyList();
        }
        File f = new File(overridePropertiesLocation);
        if (f.exists() && !f.isDirectory()) {
            try {
                return List
                    .of(new PropertiesConfigSource(f.toURI().toURL(), ConfigSource.DEFAULT_ORDINAL + 1000));
            } catch (IOException e) {
                throw new RuntimeException(
                    "OVERRIDE_PROPERTIES_URL config var had value " + overridePropertiesLocation
                        + "that is not a valid url according to new File(overridePropertiesLocation).toURI().toURL().",
                    e);
            }
        } else {
            LOGGER.warn("OVERRIDE_PROPERTIES_LOCATION env var refer to a location not existent or that is not a file: {}",
                overridePropertiesLocation);
        }
        return Collections.emptyList();
    }
}
