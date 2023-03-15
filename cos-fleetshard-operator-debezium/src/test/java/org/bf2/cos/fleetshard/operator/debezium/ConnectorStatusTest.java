package org.bf2.cos.fleetshard.operator.debezium;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ConnectorStatusTest {

    public static Stream<Arguments> computeConnectStatus() {
        return Stream.of(
            // connect
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("Ignored")
                    .withMessage("kafka connect not ready")
                    .build(),
                null,
                ManagedConnector.STATE_PROVISIONING,
                "False",
                "ConnectorNotReady",
                "kafka connect not ready"),
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("False")
                    .withReason("Error")
                    .withMessage("kafka connect failed")
                    .build(),
                null,
                ManagedConnector.STATE_FAILED,
                "False",
                "Error",
                "kafka connect failed"),
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                null,
                ManagedConnector.STATE_PROVISIONING,
                "False",
                "ConnectorNotReady",
                "ConnectorNotReady"),
            // connector
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .withStatus("False")
                    .withReason("Ignored")
                    .withMessage("kafka connector not ready")
                    .build(),
                ManagedConnector.STATE_PROVISIONING,
                "False",
                "ConnectorNotReady",
                "kafka connector not ready"),
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .withStatus("False")
                    .withReason("Error")
                    .withMessage("kafka connector failed")
                    .build(),
                ManagedConnector.STATE_FAILED,
                "False",
                "Error",
                "kafka connector failed"),
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .withStatus("False")
                    .withReason("Paused")
                    .withMessage("kafka connector paused")
                    .build(),
                ManagedConnector.STATE_STOPPED,
                "False",
                "ConnectorPaused",
                "kafka connector paused"),
            arguments(
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .withStatus("True")
                    .withReason("Ready")
                    .withMessage("Ready")
                    .build(),
                ManagedConnector.STATE_READY,
                "False",
                DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY,
                "Ready")

        );
    }

    @ParameterizedTest
    @MethodSource
    void computeConnectStatus(
        Condition connectCondition,
        Condition connectorCondition,
        String expectedPhase,
        String expectedStatus,
        String expectedReason,
        String expectedMessage) {

        ManagedConnector connector = new ManagedConnector();
        connector.setStatus(new ManagedConnectorStatus());
        connector.getStatus().setConnectorStatus(new ConnectorStatusSpec());
        connector.getStatus().getConnectorStatus().setConditions(new ArrayList<>(2));

        if (connectCondition != null) {
            connector.getStatus().getConnectorStatus().getConditions().add(connectCondition);
        }
        if (connectorCondition != null) {
            connector.getStatus().getConnectorStatus().getConditions().add(connectorCondition);
        }

        DebeziumOperandSupport.computeConnectorCondition(connector);

        assertThat(connector.getStatus().getConnectorStatus().getPhase()).isEqualTo(expectedPhase);

        if (expectedStatus != null) {
            assertThat(connector.getStatus().getConnectorStatus().getConditions())
                .anySatisfy(condition -> {
                    assertThat(condition.getType()).isEqualTo("Ready", "ready");
                    assertThat(condition.getStatus()).isEqualTo(expectedStatus, "status");
                    assertThat(condition.getReason()).isEqualTo(expectedReason, "reason");
                    assertThat(condition.getMessage()).isEqualTo(expectedMessage, "message");
                });
        }
    }
}
