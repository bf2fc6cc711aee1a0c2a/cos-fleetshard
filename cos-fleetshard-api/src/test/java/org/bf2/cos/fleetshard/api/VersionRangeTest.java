package org.bf2.cos.fleetshard.api;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionRangeTest {
    @Test
    void versionRangeFromString() {
        Assertions.assertThat(new VersionRange("[1.2.3,4.5.6)"))
            .hasFieldOrPropertyWithValue("leftIndicator", '[')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ')');
        Assertions.assertThat(new VersionRange("(1.2.3,4.5.6)"))
            .hasFieldOrPropertyWithValue("leftIndicator", '(')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ')');
        Assertions.assertThat(new VersionRange("(1.2.3,4.5.6]"))
            .hasFieldOrPropertyWithValue("leftIndicator", '(')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ']');
        Assertions.assertThat(new VersionRange("[1.2.3,4.5.6]"))
            .hasFieldOrPropertyWithValue("leftIndicator", '[')
            .hasFieldOrPropertyWithValue("leftVersion", new Version(1, 2, 3))
            .hasFieldOrPropertyWithValue("rightVersion", new Version(4, 5, 6))
            .hasFieldOrPropertyWithValue("rightIndicator", ']');
    }

    @Test
    void throwsExceptionOnWrongFormat() {
        Assertions.assertThatThrownBy(() -> new VersionRange("<1.2.3,4.5.6)"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new VersionRange("1.2.3,4.5.6"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new VersionRange("(1.2.3,4.5.6"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new VersionRange("1.2.3,4.5.6)"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new VersionRange("1.2.3"))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> new VersionRange("[1.2,4.5)"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void versionRangeIncludes() {
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("2.3.4")));
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("2.3.4.b1")));
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("[1.2.3,4.5.6)").includes(new Version("1.2.3")));
        org.junit.jupiter.api.Assertions.assertFalse(new VersionRange("(1.2.3,4.5.6)").includes(new Version("1.2.3")));
        org.junit.jupiter.api.Assertions.assertFalse(new VersionRange("(1.2.3,4.5.6)").includes(new Version("4.5.6")));
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("(1.2.3,4.5.6]").includes(new Version("4.5.6")));
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("(1.2.3,0)").includes(new Version("2.3.4")));
        org.junit.jupiter.api.Assertions.assertTrue(new VersionRange("(1.2.3,)").includes(new Version("2.3.4")));
    }
}
