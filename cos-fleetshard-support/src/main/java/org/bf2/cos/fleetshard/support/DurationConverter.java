package org.bf2.cos.fleetshard.support;

import java.time.Duration;
import java.util.Objects;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DurationConverter implements Converter<Duration> {
    @Override
    public Duration convert(String value) throws IllegalArgumentException, NullPointerException {
        if (value.isEmpty()) {
            return Duration.ZERO;
        }
        if (Objects.equals("disabled", value)) {
            return Duration.ZERO;
        }
        if (Objects.equals("off", value)) {
            return Duration.ZERO;
        }

        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }
        try {
            return Duration.parse(value);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid duration: " + value, e);
        }
    }
}
