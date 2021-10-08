package org.bf2.cos.fleetshard.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionRange {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^([(\\[])([a-zA-Z0-9_\\-.]+),([a-zA-Z0-9_\\-.]+)?([)\\]])$");

    private final char leftIndicator;
    private final Version leftVersion;
    private final Version rightVersion;
    private final char rightIndicator;

    public VersionRange(char leftIndicator, Version leftVersion, Version rightVersion, char rightIndicator) {
        this.leftIndicator = leftIndicator;
        this.leftVersion = leftVersion;
        this.rightVersion = rightVersion;
        this.rightIndicator = rightIndicator;
    }

    public VersionRange(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported version format: " + version);
        }
        if (matcher.groupCount() != 4) {
            throw new IllegalArgumentException("Unsupported version format: " + version);
        }

        this.leftIndicator = matcher.group(1).charAt(0);
        this.leftVersion = new Version(matcher.group(2));
        this.rightVersion = matcher.group(3) == null || "0".equals(matcher.group(3)) ? null : new Version(matcher.group(3));
        this.rightIndicator = matcher.group(4).charAt(0);
    }

    public char getLeftIndicator() {
        return leftIndicator;
    }

    public Version getLeftVersion() {
        return leftVersion;
    }

    public Version getRightVersion() {
        return rightVersion;
    }

    public char getRightIndicator() {
        return rightIndicator;
    }

    public boolean includes(Version version) {
        if (leftVersion.compareTo(version) >= (leftIndicator == '[' ? 1 : 0)) {
            return false;
        }
        if (rightVersion == null) {
            return true;
        }
        return rightVersion.compareTo(version) >= (rightIndicator == ']' ? 0 : 1);
    }
}
