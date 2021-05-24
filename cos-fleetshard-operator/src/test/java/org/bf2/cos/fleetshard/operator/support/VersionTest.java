package org.bf2.cos.fleetshard.operator.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VersionTest {
    @Test
    void versionCanBeCompared() {
        var v1 = new Version(1, 1, 0);
        var v2 = new Version(1, 2, 0);
        var v3 = new Version(1, 2, 3);

        assertThat(v1).isLessThan(v2);
        assertThat(v2).isGreaterThan(v1);
        assertThat(v2).isGreaterThan(v1).isLessThan(v3);
    }

    @Test
    void versionWithQualifier() {
        var v1 = new Version(1, 1, 0, "q0");
        var v2 = new Version(1, 1, 0, "q1");

        assertThat(v1).isLessThan(v2);
    }

    @Test
    void versionFromString() {
        var v1 = new Version(1, 2, 3, "q0");
        var v2 = new Version("1.2.3.q0");

        assertThat(v1.getMajor()).isEqualTo(1);
        assertThat(v1.getMinor()).isEqualTo(2);
        assertThat(v1.getMicro()).isEqualTo(3);
        assertThat(v1.getQualifier()).isEqualTo("q0");
        assertThat(v1).isEqualTo(v2);

        assertThat(new Version("1.2.3.q1"))
            .isLessThan(new Version("1.2.4.q0"))
            .isEqualTo(new Version("1.2.3.q1"))
            .isGreaterThan(new Version("1.2.3.q0"));

        assertThat(new Version("1.2.3"))
            .hasFieldOrPropertyWithValue("major", 1)
            .hasFieldOrPropertyWithValue("minor", 2)
            .hasFieldOrPropertyWithValue("micro", 3)
            .hasFieldOrPropertyWithValue("qualifier", null);

        assertThat(new Version("1.2.3.q1"))
            .hasFieldOrPropertyWithValue("major", 1)
            .hasFieldOrPropertyWithValue("minor", 2)
            .hasFieldOrPropertyWithValue("micro", 3)
            .hasFieldOrPropertyWithValue("qualifier", "q1");
    }

    @Test
    void throwsExceptionOnWrongFormat() {
        assertThatThrownBy(() -> new Version("1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Version("1.2"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Version("1.2.3."))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Version("1.2.3.q1.q2"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
