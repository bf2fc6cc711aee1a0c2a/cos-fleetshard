package org.bf2.cos.fleetshard.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorSelector {
    private String type;
    private String version;

    public OperatorSelector() {
        this(null, null);

    }

    public OperatorSelector(String type, String version) {
        this.type = type;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Optional<Operator> select(Collection<Operator> operators) {
        if (operators == null) {
            return Optional.empty();
        }
        if (operators.isEmpty()) {
            return Optional.empty();
        }

        final VersionRange range = new VersionRange(version);
        final Comparator<Operator> cmp = Comparator.comparing(o -> new Version(o.getVersion()));

        return operators
            .stream()
            .filter(o -> Objects.equals(o.getType(), type))
            .filter(o -> range.includes(new Version(o.getVersion())))
            .max(cmp);
    }
}
