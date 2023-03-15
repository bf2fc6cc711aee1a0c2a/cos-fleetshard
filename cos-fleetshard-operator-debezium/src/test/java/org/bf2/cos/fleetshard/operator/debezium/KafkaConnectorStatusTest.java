package org.bf2.cos.fleetshard.operator.debezium;

import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetail;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetailBuilder;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KafkaConnectorStatusTest {

    public static Stream<Arguments> computeConnectorStatus() {
        return Stream.of(
            //
            // Connector
            //
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_FAILED, null))
                    .build(),
                "False",
                "Error",
                "DebeziumException"),
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_FAILED, "connector failure"))
                    .build(),
                "False",
                "Error",
                "connector failure"),
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_UNASSIGNED, null))
                    .build(),
                "False",
                "Unassigned",
                "The connector is not yet assigned or re-balancing is in progress"),
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_PAUSED, null))
                    .build(),
                "False",
                "Paused",
                "The connector is paused"),

            //
            // Tasks
            //
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_RUNNING, null))
                    .addNewTask(KafkaConnectorDetail.STATE_FAILED, null)
                    .build(),
                "False",
                "Error",
                "DebeziumException"),
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_RUNNING, null))
                    .addNewTask(KafkaConnectorDetail.STATE_FAILED, "task failure")
                    .build(),
                "False",
                "Error",
                "task failure"),

            //
            // Misc
            //
            arguments(
                new KafkaConnectorDetailBuilder()
                    .withConnector(new KafkaConnectorStatus(KafkaConnectorDetail.STATE_FAILED, "connector failure"))
                    .addNewTask(KafkaConnectorDetail.STATE_FAILED, "task failure")
                    .build(),
                "False",
                "Error",
                "connector failure")

        );
    }

    @ParameterizedTest
    @MethodSource
    void computeConnectorStatus(
        KafkaConnectorDetail detail,
        String expectedStatus,
        String expectedReason,
        String expectedMessage) {

        ManagedConnector connector = new ManagedConnector();
        connector.setStatus(new ManagedConnectorStatus());

        DebeziumOperandSupport.computeKafkaConnectorCondition(
            connector,
            detail);

        assertThat(connector.getStatus().getConnectorStatus().getConditions())
            .anySatisfy(condition -> {
                assertThat(condition)
                    .hasFieldOrPropertyWithValue("type", DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .hasFieldOrPropertyWithValue("status", expectedStatus)
                    .hasFieldOrPropertyWithValue("reason", expectedReason)
                    .hasFieldOrPropertyWithValue("message", expectedMessage);
            });
    }
}
