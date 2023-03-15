package org.bf2.cos.fleetshard.operator.debezium;

import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KafkaConnectStatusTest {

    public static Stream<Arguments> computeConnectStatus() {
        return Stream.of(
            arguments(
                null,
                null,
                "False",
                "DeploymentNotReady",
                "DeploymentNotReady"),
            arguments(
                new DeploymentBuilder().build(),
                null,
                "False",
                "DeploymentNotReady",
                "DeploymentNotReady"),
            arguments(
                new DeploymentBuilder().withStatus(new DeploymentStatus()).build(),
                null,
                "False",
                "PodNotReady",
                "PodNotReady"),
            arguments(
                new DeploymentBuilder().withStatus(new DeploymentStatus()).build(),
                new PodBuilder().build(),
                "False",
                "PodNotReady",
                "PodNotReady"),
            arguments(
                new DeploymentBuilder().withSpec(new DeploymentSpec()).withStatus(new DeploymentStatus()).build(),
                new PodBuilder().withStatus(new PodStatus()).build(),
                "False",
                "NotEnoughReplicas",
                "NotEnoughReplicas"),
            arguments(
                new DeploymentBuilder()
                    .withSpec(new io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder()
                        .withReplicas(1)
                        .build())
                    .withStatus(new DeploymentStatus())
                    .build(),
                new PodBuilder().withStatus(new PodStatus()).build(),
                "False",
                "NotEnoughReplicas",
                "NotEnoughReplicas"),
            arguments(
                new DeploymentBuilder()
                    .withSpec(new io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder()
                        .withReplicas(1)
                        .build())
                    .withStatus(new DeploymentStatusBuilder()
                        .withReplicas(0)
                        .build())
                    .build(),
                new PodBuilder().withStatus(new PodStatus()).build(),
                "False",
                "NotEnoughReplicas",
                "NotEnoughReplicas"),
            arguments(
                new DeploymentBuilder()
                    .withSpec(new io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder()
                        .withReplicas(1)
                        .build())
                    .withStatus(new DeploymentStatusBuilder()
                        .withReadyReplicas(1)
                        .build())
                    .build(),
                new PodBuilder().withStatus(new PodStatus()).build(),
                null,
                null,
                null)

        );
    }

    @ParameterizedTest
    @MethodSource
    void computeConnectStatus(
        Deployment deployment,
        Pod pod,
        String expectedStatus,
        String expectedReason,
        String expectedMessage) {

        ManagedConnector connector = new ManagedConnector();
        connector.setStatus(new ManagedConnectorStatus());

        DebeziumOperandSupport.computeKafkaConnectCondition(
            connector,
            deployment,
            pod);

        if (expectedStatus != null) {
            assertThat(connector.getStatus().getConnectorStatus().getConditions())
                .anySatisfy(condition -> {
                    assertThat(condition)
                        .hasFieldOrPropertyWithValue("type", DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECT_READY)
                        .hasFieldOrPropertyWithValue("status", expectedStatus)
                        .hasFieldOrPropertyWithValue("reason", expectedReason)
                        .hasFieldOrPropertyWithValue("message", expectedMessage);
                });
        }
    }
}
