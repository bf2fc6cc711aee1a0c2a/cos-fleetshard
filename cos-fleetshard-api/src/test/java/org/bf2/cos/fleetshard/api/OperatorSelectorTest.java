package org.bf2.cos.fleetshard.api;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OperatorSelectorTest {
    public static final List<Operator> OPERATORS = List.of(
        new Operator("1", "camel", "1.0.0"),
        new Operator("2", "camel", "1.1.0"),
        new Operator("3", "camel", "1.9.0"),
        new Operator("4", "camel", "2.0.0"),
        new Operator("5", "strimzi", "1.9.0"),
        new Operator("6", "strimzi", "2.0.0"));

    @Test
    void assignOperator() {
        Assertions.assertThat(new OperatorSelector("2", "camel", "[1.0.0,2.0.0)").assign(OPERATORS))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.1.0");

        Assertions.assertThat(new OperatorSelector("camel", "[1.0.0,2.0.0)").assign(OPERATORS))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.9.0");

        assertThatThrownBy(() -> new OperatorSelector("5", "camel", "[1.0.0,2.0.0)").assign(OPERATORS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The given operator id does not match the operator selector type");

        assertThatThrownBy(() -> new OperatorSelector("4", "camel", "[1.0.0,2.0.0)").assign(OPERATORS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The given operator id is outside the operator selector range");
    }

    @Test
    void availableOperator() {

        Assertions.assertThat(new OperatorSelector("strimzi", "[1.0.0,2.0.0)").available(OPERATORS))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "strimzi")
            .hasFieldOrPropertyWithValue("version", "1.9.0");

        Assertions.assertThat(new OperatorSelector("camel", "[1.0.0,2.0.0)").available(OPERATORS))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.9.0");

        Assertions.assertThat(new OperatorSelector("camel", "(1.0.0,1.8.9)").available(OPERATORS))
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("type", "camel")
            .hasFieldOrPropertyWithValue("version", "1.1.0");
    }
}
