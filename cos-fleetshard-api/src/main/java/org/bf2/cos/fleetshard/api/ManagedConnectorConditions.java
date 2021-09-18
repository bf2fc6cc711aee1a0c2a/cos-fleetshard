package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.Condition;

public final class ManagedConnectorConditions {
    private ManagedConnectorConditions() {
    }

    public static void clearConditions(ManagedConnector connector) {
        if (connector.getStatus().getConditions() != null) {
            connector.getStatus().getConditions().clear();
        }
    }

    public static boolean setCondition(ManagedConnector connector, Type type, Status status, String reason, String message) {
        Condition condition = new Condition();
        condition.setType(type.name());
        condition.setStatus(status.name());
        condition.setReason(reason);
        condition.setMessage(message);
        condition.setLastTransitionTime(Conditions.now());

        return setCondition(connector, condition);
    }

    public static boolean setCondition(ManagedConnector connector, Condition condition) {
        if (connector.getStatus().getConditions() == null) {
            connector.getStatus().setConditions(new ArrayList<>());
        }

        for (int i = 0; i < connector.getStatus().getConditions().size(); i++) {
            final Condition current = connector.getStatus().getConditions().get(i);

            if (Objects.equals(current.getType(), condition.getType())) {
                boolean update = !Objects.equals(condition.getStatus(), current.getStatus())
                    || !Objects.equals(condition.getReason(), current.getReason())
                    || !Objects.equals(condition.getMessage(), current.getMessage());

                if (update) {
                    connector.getStatus().getConditions().set(i, condition);
                }

                connector.getStatus().getConditions().sort(Comparator.comparing(Condition::getLastTransitionTime));

                return update;
            }
        }

        connector.getStatus().getConditions().add(condition);
        connector.getStatus().getConditions().sort(Comparator.comparing(Condition::getLastTransitionTime));

        return true;
    }

    public static boolean hasCondition(ManagedConnector connector, Type type) {
        if (connector.getStatus().getConditions() == null) {
            return false;
        }

        return connector.getStatus().getConditions().stream().anyMatch(
            c -> Objects.equals(c.getType(), type.name()));
    }

    public static boolean hasCondition(ManagedConnector connector, Type type, Status status) {
        if (connector.getStatus().getConditions() == null) {
            return false;
        }

        return connector.getStatus().getConditions().stream().anyMatch(
            c -> Objects.equals(c.getType(), type.name()) && Objects.equals(c.getStatus(), status.name()));
    }

    public enum Type {
        Error,
        Ready,
        Initialization,
        Augmentation,
        Monitor,
        Delete,
        Stop
    }

    public enum Status {
        True,
        False,
        Unknown
    }
}
