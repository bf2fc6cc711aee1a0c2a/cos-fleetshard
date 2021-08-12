package org.bf2.cos.fleetshard.api;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    private static final Pattern VERSION_PATTERN = Pattern
        .compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.([a-zA-Z0-9_-]+))?$");

    private final int major;
    private final int minor;
    private final int micro;
    private final String qualifier;

    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    public Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier == null ? "" : qualifier;
    }

    public Version(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported version format: " + version);
        }
        if (matcher.groupCount() != 4) {
            throw new IllegalArgumentException("Unsupported version format: " + version);
        }

        this.major = Integer.parseInt(matcher.group(1));
        this.minor = Integer.parseInt(matcher.group(2));
        this.micro = Integer.parseInt(matcher.group(3));
        this.qualifier = matcher.group(4) == null ? "" : matcher.group(4);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public String getQualifier() {
        return qualifier;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.major).append('.');
        sb.append(this.minor).append('.');
        sb.append(this.micro);

        if (this.qualifier != null && this.qualifier.length() > 0) {
            sb.append('.').append(this.qualifier);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        Version version = (Version) o;
        return major == version.major
            && minor == version.minor
            && micro == version.micro
            && Objects.equals(qualifier, version.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro, qualifier);
    }

    @Override
    public int compareTo(Version other) {
        if (other == this) {
            return 0;
        }

        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        return qualifier.compareTo(other.qualifier);
    }
}
