package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.api.OperatorSelector;
import org.bf2.cos.fleetshard.api.Version;
import org.bf2.cos.fleetshard.api.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OperatorSelectorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorSelectorUtil.class);

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
            .filter(o -> versionInRange(range, o))
            .max(cmp);
    }

    private static boolean versionInRange(final VersionRange range, Operator o) {
        Version version;
        try {
            version = new Version(o.getVersion());
        } catch (IllegalArgumentException iae) {
            LOGGER.info("CR with unsupported version found for operator {}", o.getId());
            return false;
        }
        return range.includes(version);
    }
}
