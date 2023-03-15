package org.bf2.cos.fleetshard.support.resources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Condition;

public final class Conditions {
    private Conditions() {
    }

    public static void set(List<Condition> conditions, Condition condition) {

        for (Condition c : conditions) {
            if (Objects.equals(c.getType(), condition.getType())) {

                if (!Objects.equals(c.getStatus(), condition.getStatus())) {
                    c.setLastTransitionTime(org.bf2.cos.fleetshard.api.Conditions.now());
                }

                c.setType(condition.getType());
                c.setStatus(condition.getStatus());
                c.setReason(condition.getReason());
                c.setMessage(condition.getMessage());

                return;
            }
        }

        Condition toAdd = new Condition();
        toAdd.setType(condition.getType());
        toAdd.setStatus(condition.getStatus());
        toAdd.setReason(condition.getReason());
        toAdd.setMessage(condition.getMessage());
        toAdd.setLastTransitionTime(condition.getLastTransitionTime());

        if (toAdd.getLastTransitionTime() == null) {
            toAdd.setLastTransitionTime(org.bf2.cos.fleetshard.api.Conditions.now());
        }

        conditions.add(toAdd);
    }

    public static Optional<Condition> get(List<Condition> conditions, String type) {
        if (conditions == null) {
            return Optional.empty();
        }

        return conditions.stream().filter(c -> Objects.equals(c.getType(), type)).findFirst();
    }
}
