package org.bf2.cos.fleetshard.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OperatorSelectorTest {

    @Test
    void selectVersion() {
        var operators = List.of(
            new Operator("1", "camel", "1.0.0"),
            new Operator("2", "camel", "1.1.0"),
            new Operator("3", "camel", "1.9.0"),
            new Operator("4", "camel", "2.0.0"),
            new Operator("5", "strimzi", "1.9.0"),
            new Operator("6", "strimzi", "2.0.0"));

        assertThat(new OperatorSelector("strimzi", "[1.0.0,2.0.0)").select(operators))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "strimzi")
            .hasFieldOrPropertyWithValue("version", "1.9.0");

        assertThat(new OperatorSelector("camel", "[1.0.0,2.0.0)").select(operators))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.9.0");

        assertThat(new OperatorSelector("camel", "(1.0.0,1.8.9)").select(operators))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.1.0");
    }
}
