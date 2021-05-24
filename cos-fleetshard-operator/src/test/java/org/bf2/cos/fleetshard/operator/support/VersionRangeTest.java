package org.bf2.cos.fleetshard.operator.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionRangeTest {
    @Test
    void versionRangeFromString() {
        assertThat(new VersionRange("[1.2.3,4.5.6)"))
            .hasFieldOrPropertyWithValue("leftIndicator", '[')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ')');
        assertThat(new VersionRange("(1.2.3,4.5.6)"))
            .hasFieldOrPropertyWithValue("leftIndicator", '(')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ')');
        assertThat(new VersionRange("(1.2.3,4.5.6]"))
            .hasFieldOrPropertyWithValue("leftIndicator", '(')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ']');
        assertThat(new VersionRange("[1.2.3,4.5.6]"))
            .hasFieldOrPropertyWithValue("leftIndicator", '[')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ']');
    }

    @Test
    void throwsExceptionOnWrongFormat() {
        assertThatThrownBy(() -> new VersionRange("<1.2.3,4.5.6)"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VersionRange("1.2.3,4.5.6"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VersionRange("(1.2.3,4.5.6"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VersionRange("1.2.3,4.5.6)"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VersionRange("1.2.3"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VersionRange("[1.2,4.5)"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void versionRangeIncludes() {
        assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("2.3.4")));
        assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("2.3.4.b1")));
        assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("1.2.3")));
        assertFalse(new VersionRange("(1.2.3,4.5.6)").includes(new Version("1.2.3")));
        assertFalse(new VersionRange("(1.2.3,4.5.6)").includes(new Version("4.5.6")));
        assertTrue(new VersionRange("(1.2.3,4.5.6]").includes(new Version("4.5.6")));
        assertTrue(new VersionRange("(1.2.3,0)").includes(new Version("2.3.4")));
        assertTrue(new VersionRange("(1.2.3,)").includes(new Version("2.3.4")));
    }
}
