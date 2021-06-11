package org.bf2.cos.fleetshard.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorSelector {
    private String id;
    private String type;
    private String version;

    public OperatorSelector() {
        this(null, null, null);
    }

    public OperatorSelector(String type, String version) {
        this(null, type, version);
    }

    public OperatorSelector(String id, String type, String version) {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getType() {
        return type;
    }

    @JsonProperty
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty
    public String getVersion() {
        return version;
    }

    @JsonProperty
    public void setVersion(String version) {
        this.version = version;
    }

    public Optional<Operator> assign(Collection<Operator> operators) {
        if (operators == null) {
            return Optional.empty();
        }
        if (operators.isEmpty()) {
            return Optional.empty();
        }

        if (this.id != null) {
            Optional<Operator> operator = operators
                .stream()
                .filter(o -> Objects.equals(o.getId(), this.id))
                .findFirst();

            if (operator.isPresent()) {
                if (!Objects.equals(type, operator.get().getType())) {
                    throw new IllegalArgumentException(
                        "The given operator id does not match the operator selector type: "
                            + "id: " + this.id
                            + ", id_type: " + operator.get().getType()
                            + ", selector_type: " + this.type);
                }
                if (!new VersionRange(version).includes(new Version(operator.get().getVersion()))) {
                    throw new IllegalArgumentException(
                        "The given operator id is outside the operator selector range: "
                            + "id: " + this.id
                            + ", id_range: " + operator.get().getVersion()
                            + ", selector_range: " + this.version);
                }
            }

            return operator;
        }

        return available(operators);
    }

    public Optional<Operator> available(Collection<Operator> operators) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperatorSelector)) {
            return false;
        }
        OperatorSelector selector = (OperatorSelector) o;
        return Objects.equals(getId(), selector.getId())
            && Objects.equals(getType(), selector.getType())
            && Objects.equals(getVersion(), selector.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getId(),
            getType(),
            getVersion());
    }
}
