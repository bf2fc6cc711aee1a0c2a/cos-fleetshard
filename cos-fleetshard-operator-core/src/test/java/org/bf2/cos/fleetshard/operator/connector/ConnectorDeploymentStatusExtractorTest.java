package org.bf2.cos.fleetshard.operator.connector;

import java.util.stream.Stream;

import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatusBuilder;
import org.bf2.cos.fleetshard.operator.client.MetaClient;
import org.bf2.cos.fleetshard.support.unstructured.UnstructuredClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_READY;
import static org.bf2.cos.fleetshard.api.ManagedConnector.DESIRED_STATE_STOPPED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DELETED;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_DE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING;
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_STOPPED;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConnectorDeploymentStatusExtractorTest {

    public static Stream<Arguments> specAndNoStatus() {
        return Stream.of(
            arguments(
                DESIRED_STATE_READY,
                STATE_PROVISIONING),
            arguments(
                DESIRED_STATE_STOPPED,
                STATE_DE_PROVISIONING),
            arguments(
                DESIRED_STATE_DELETED,
                STATE_DE_PROVISIONING));
    }

    public static Stream<Arguments> specAndStatusWithMonitorPhase() {
        return Stream.of(
            arguments(
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_PROVISIONING),
            arguments(
                DESIRED_STATE_READY,
                DESIRED_STATE_STOPPED,
                STATE_DE_PROVISIONING),
            arguments(
                DESIRED_STATE_READY,
                DESIRED_STATE_DELETED,
                STATE_DE_PROVISIONING));
    }

    public static Stream<Arguments> phaseHasPriority() {
        return Stream.of(
            arguments(
                ManagedConnectorStatus.PhaseType.Initialization,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_PROVISIONING),
            arguments(
                ManagedConnectorStatus.PhaseType.Augmentation,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_PROVISIONING),
            arguments(
                ManagedConnectorStatus.PhaseType.Deleting,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_DE_PROVISIONING),
            arguments(
                ManagedConnectorStatus.PhaseType.Deleted,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_DELETED),
            arguments(
                ManagedConnectorStatus.PhaseType.Stopping,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_DE_PROVISIONING),
            arguments(
                ManagedConnectorStatus.PhaseType.Stopped,
                DESIRED_STATE_READY,
                DESIRED_STATE_READY,
                STATE_STOPPED));
    }

    public static Stream<Arguments> defaultIsPhaseIsNotSet() {
        return Stream.of(
            arguments(
                DESIRED_STATE_READY,
                STATE_PROVISIONING),
            arguments(
                DESIRED_STATE_STOPPED,
                STATE_DE_PROVISIONING),
            arguments(
                DESIRED_STATE_DELETED,
                STATE_DE_PROVISIONING));
    }

    /*
     * Test that if the status sub resource is not provided, then the status
     * extractor takes into account the spec.
     *
     * Meta service is not involved.
     */
    @ParameterizedTest
    @MethodSource
    public void specAndNoStatus(
        String desiredState,
        String expextedState) {

        final var meta = Mockito.mock(MetaClient.class);
        final var uc = Mockito.mock(UnstructuredClient.class);
        final var extractor = new ConnectorDeploymentStatusExtractor(uc, meta);

        var status = extractor.extract(
            new ManagedConnectorBuilder()
                .withSpec(new ManagedConnectorSpecBuilder()
                    .withDeployment(new DeploymentSpecBuilder()
                        .withDeploymentResourceVersion(2L)
                        .withDesiredState(desiredState)
                        .build())
                    .build())
                .withStatus(null)
                .build());

        assertThat(status.getPhase()).isEqualTo(expextedState);
        assertThat(status.getOperators()).isNull();
        assertThat(status.getConditions()).isNullOrEmpty();
        assertThat(status.getResourceVersion()).isEqualTo(2L);

        verify(meta, times(0)).status(any(), any());
    }

    /*
     * Test that if the status sub resource is provided, then the status
     * extractor ignores the spec.
     *
     * Meta service is not involved.
     */
    @ParameterizedTest
    @MethodSource
    void specAndStatusWithMonitorPhase(
        String specDesiredState,
        String statusDesiredState,
        String expectedState) {

        var meta = Mockito.mock(MetaClient.class);
        var uc = Mockito.mock(UnstructuredClient.class);
        var extractor = new ConnectorDeploymentStatusExtractor(uc, meta);

        var status = extractor.extract(new ManagedConnectorBuilder()
            .withSpec(new ManagedConnectorSpecBuilder()
                .withDeployment(new DeploymentSpecBuilder()
                    .withDeploymentResourceVersion(2L)
                    .withDesiredState(specDesiredState)
                    .build())
                .build())
            .withStatus(new ManagedConnectorStatusBuilder()
                .withPhase(ManagedConnectorStatus.PhaseType.Monitor)
                .withDeployment(new DeploymentSpecBuilder()
                    .withDeploymentResourceVersion(1L)
                    .withDesiredState(statusDesiredState)
                    .build())
                .build())
            .build());

        assertThat(status.getPhase()).isEqualTo(expectedState);
        assertThat(status.getConditions()).isNullOrEmpty();
        assertThat(status.getResourceVersion()).isEqualTo(1L);

        assertThat(status)
            .extracting(ConnectorDeploymentStatus::getOperators)
            .hasAllNullFieldsOrProperties();

        verify(meta, times(0)).status(any(), any());
    }

    /*
     * Test that if the status sub resource is provided and the phase is not
     * "monitor", then the status extractor ignores the spec and meta is noy.
     * involved as the phase is computed based on the ManagedConnector phase.
     *
     * Meta service is not involved.
     */
    @ParameterizedTest
    @MethodSource
    void phaseHasPriority(
        ManagedConnectorStatus.PhaseType phase,
        String specDesiredState,
        String statusDesiredState,
        String expectedState) {

        var meta = Mockito.mock(MetaClient.class);
        var uc = Mockito.mock(UnstructuredClient.class);
        var extractor = new ConnectorDeploymentStatusExtractor(uc, meta);

        var status = extractor.extract(new ManagedConnectorBuilder()
            .withSpec(new ManagedConnectorSpecBuilder()
                .withDeployment(new DeploymentSpecBuilder()
                    .withDeploymentResourceVersion(2L)
                    .withDesiredState(specDesiredState)
                    .build())
                .build())
            .withStatus(new ManagedConnectorStatusBuilder()
                .withPhase(phase)
                .withDeployment(new DeploymentSpecBuilder()
                    .withDeploymentResourceVersion(1L)
                    .withDesiredState(statusDesiredState)
                    .build())
                .build())
            .build());

        assertThat(status.getPhase()).isEqualTo(expectedState);
        assertThat(status.getConditions()).isNullOrEmpty();
        assertThat(status.getResourceVersion()).isEqualTo(1L);
        assertThat(status.getOperators()).isNull();

        verify(meta, times(0)).status(any(), any());
    }

    /*
     * Test that if no phase can be computed by the meta service, then phase is set
     * according to an transient phase according to the desired state.
     *
     * Meta service is not involved.
     */
    @ParameterizedTest
    @MethodSource
    void defaultIsPhaseIsNotSet(
        String statusDesiredState,
        String expectedState) {

        var meta = Mockito.mock(MetaClient.class);
        var uc = Mockito.mock(UnstructuredClient.class);
        var extractor = new ConnectorDeploymentStatusExtractor(uc, meta);

        var status = extractor.extract(new ManagedConnectorBuilder()
            .withStatus(new ManagedConnectorStatusBuilder()
                .withPhase(ManagedConnectorStatus.PhaseType.Monitor)
                .withDeployment(new DeploymentSpecBuilder()
                    .withDeploymentResourceVersion(1L)
                    .withDesiredState(statusDesiredState)
                    .build())
                .build())
            .build());

        assertThat(status.getPhase()).isEqualTo(expectedState);
        assertThat(status.getConditions()).isNullOrEmpty();
        assertThat(status.getResourceVersion()).isEqualTo(1L);

        assertThat(status)
            .extracting(ConnectorDeploymentStatus::getOperators)
            .hasAllNullFieldsOrProperties();

        verify(meta, times(0)).status(any(), any());
    }
}
