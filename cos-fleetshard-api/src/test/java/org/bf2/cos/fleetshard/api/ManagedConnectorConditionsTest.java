package org.bf2.cos.fleetshard.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Condition;

public class ManagedConnectorConditionsTest {

    @Test
    void conditionLookup() {
        ManagedConnector connector = new ManagedConnector();

        assertThat(ManagedConnectorConditions.hasCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.True)).isFalse();

        Condition condition = new Condition();
        condition.setType("Ready");
        condition.setStatus("True");
        condition.setReason("reason");
        condition.setMessage("message");
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        connector.getStatus().setConditions(List.of(condition));

        assertThat(ManagedConnectorConditions.hasCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.True)).isTrue();

        assertThat(ManagedConnectorConditions.hasCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.False)).isFalse();

        assertThat(ManagedConnectorConditions.hasCondition(
            connector,
            ManagedConnectorConditions.Type.Ready)).isTrue();
    }

    @Test
    void conditionAdd() {
        ManagedConnector connector = new ManagedConnector();

        Condition condition = new Condition();
        condition.setType("Ready");
        condition.setStatus("True");
        condition.setReason("reason");
        condition.setMessage("message");
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        connector.getStatus().setConditions(List.of(condition));

        assertThat(ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.True,
            "new reason",
            "new message")).isTrue();

        assertThat(connector.getStatus().getConditions())
            .hasSize(1)
            .allMatch(c -> {
                return Objects.equals(c.getType(), "Ready")
                    && Objects.equals(c.getStatus(), "True")
                    && Objects.equals(c.getReason(), "new reason")
                    && Objects.equals(c.getMessage(), "new message")
                    && !Objects.equals(c.getLastTransitionTime(), condition.getLastTransitionTime());
            });

        assertThat(ManagedConnectorConditions.setCondition(
            connector,
            ManagedConnectorConditions.Type.Ready,
            ManagedConnectorConditions.Status.True,
            "new reason",
            "new message")).isFalse();
    }
}
