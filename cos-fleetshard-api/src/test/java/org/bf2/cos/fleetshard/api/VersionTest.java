package org.bf2.cos.fleetshard.api;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {
    @Test
    void versionCanBeCompared() {
        var v1 = new Version(1, 1, 0);
        var v2 = new Version(1, 2, 0);
        var v3 = new Version(1, 2, 3);

        Assertions.assertThat(v1).isLessThan(v2);
        Assertions.assertThat(v2).isGreaterThan(v1);
        Assertions.assertThat(v2).isGreaterThan(v1).isLessThan(v3);
    }

    @Test
    void versionWithQualifier() {
        var v1 = new Version(1, 1, 0, "q0");
        var v2 = new Version(1, 1, 0, "q1");

        Assertions.assertThat(v1).isLessThan(v2);
    }

    @Test
    void versionFromString() {
        var v1 = new Version(1, 2, 3, "q0");
        var v2 = new Version("1.2.3.q0");

        Assertions.assertThat(v1.getMajor()).isEqualTo(1);
        Assertions.assertThat(v1.getMinor()).isEqualTo(2);
        Assertions.assertThat(v1.getMicro()).isEqualTo(3);
        Assertions.assertThat(v1.getQualifier()).isEqualTo("q0");
        Assertions.assertThat(v1).isEqualTo(v2);

        Assertions.assertThat(new Version("1.2.3.q1"))
            .isLessThan(new Version("1.2.4.q0"))
            .isEqualTo(new Version("1.2.3.q1"))
            .isGreaterThan(new Version("1.2.3.q0"));

        Assertions.assertThat(new Version("1.2.3"))
            .hasFieldOrPropertyWithValue("major", 1)
            .hasFieldOrPropertyWithValue("minor", 2)
            .hasFieldOrPropertyWithValue("micro", 3)
            .hasFieldOrPropertyWithValue("qualifier", null);

        Assertions.assertThat(new Version("1.2.3.q1"))
            .hasFieldOrPropertyWithValue("major", 1)
            .hasFieldOrPropertyWithValue("minor", 2)
            .hasFieldOrPropertyWithValue("micro", 3)
            .hasFieldOrPropertyWithValue("qualifier", "q1");
    }

    @Test
    void throwsExceptionOnWrongFormat() {
        Assertions.assertThatThrownBy(() -> new Version("1"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new Version("1.2"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new Version("1.2.3."))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new Version("1.2.3.q1.q2"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
