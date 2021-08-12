package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.api.VersionRange;

public final class OperatorSelectorUtil {
    private OperatorSelectorUtil() {
    }

    public static Optional<Operator> assign(OperatorSelector selector, Collection<Operator> operators) {
        if (operators == null) {
            return Optional.empty();
        }
        if (operators.isEmpty()) {
            return Optional.empty();
        }

        if (selector.getId() != null) {
            Optional<Operator> operator = operators
                .stream()
                .filter(o -> Objects.equals(o.getId(), selector.getId()))
                .findFirst();

            if (operator.isPresent()) {
                if (!Objects.equals(selector.getType(), operator.get().getType())) {
                    throw new IllegalArgumentException(
                        "The given operator id does not match the operator selector type: "
                            + "id: " + selector.getId()
                            + ", id_type: " + operator.get().getType()
                            + ", selector_type: " + selector.getType());
                }
                if (!new VersionRange(selector.getVersion()).includes(new Version(operator.get().getVersion()))) {
                    throw new IllegalArgumentException(
                        "The given operator id is outside the operator selector range: "
                            + "id: " + selector.getId()
                            + ", id_range: " + operator.get().getVersion()
                            + ", selector_range: " + selector.getType());
                }
            }

            return operator;
        }

        return available(selector, operators);
    }

    public static Optional<Operator> available(OperatorSelector selector, Collection<Operator> operators) {
        if (operators == null) {
            return Optional.empty();
        }
        if (operators.isEmpty()) {
            return Optional.empty();
        }

        final VersionRange range = new VersionRange(selector.getVersion());
        final Comparator<Operator> cmp = Comparator.comparing(o -> new Version(o.getVersion()));

        return operators
            .stream()
            .filter(o -> Objects.equals(o.getType(), selector.getType()))
            .filter(o -> range.includes(new Version(o.getVersion())))
            .max(cmp);
    }
}
