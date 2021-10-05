package org.bf2.cos.fleetshard.operator.connector;

import java.util.stream.Stream;

import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatusBuilder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.processing.event.EventSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

public class ConnectorSecretEventTest {

    public static Stream<Arguments> filterOnUoW() {
        return Stream.of(
            arguments("1", null, "1", true),
            arguments("1", "1", "1", false),
            arguments("2", "1", "2", true));
    }

    @ParameterizedTest
    @MethodSource
    public void filterOnUoW(String connectorSpecUow, String connectorStatusUow, String secretUow, Boolean result) {
        var connector = new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(Resources.LABEL_UOW, connectorSpecUow)
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withDeployment(new DeploymentSpecBuilder()
                    .withUnitOfWork(connectorSpecUow)
                    .build())
                .build())
            .withStatus(new ManagedConnectorStatusBuilder()
                .withDeployment(new DeploymentSpecBuilder()
                    .withUnitOfWork(connectorStatusUow)
                    .build())
                .build())
            .build();

        var secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(Resources.LABEL_UOW, secretUow)
                .build())
            .build();

        var accepted = new ConnectorSecretEvent(mock(EventSource.class), secret)
            .getCustomResourcesSelector()
            .test(connector);

        assertThat(accepted).isEqualTo(result);
    }
}
