package org.bf2.cos.fleetshard.operator.camel;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "cos.operator.camel")
public interface CamelOperandConfiguration {
    List<Configuration> configurations();

    interface Configuration {
        String type();

        String value();
    }
}
