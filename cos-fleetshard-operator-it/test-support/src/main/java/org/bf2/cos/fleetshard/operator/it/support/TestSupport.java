package org.bf2.cos.fleetshard.operator.it.support;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class TestSupport {
    @KubernetesTestServer
    protected KubernetesServer ksrv;

    @Inject
    protected FleetManagerMock fm;

    @ConfigProperty(
        name = "cluster-id")
    protected String clusterId;

    @ConfigProperty(
        name = "test.namespace")
    protected String namespace;

    public static ManagedConnectorOperator newConnectorOperator(
        String namespace,
        String name,
        String type,
        String version,
        String connectorsMeta) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withNamespace(namespace)
                .withName(name)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withType(type)
                .withVersion(version)
                .withMetaService(connectorsMeta)
                .build())
            .build();
    }

    public void await(long timeout, TimeUnit unit, ThrowingRunnable condition) {
        Awaitility.await()
            .atMost(timeout, unit)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(condition);
    }

    public void await(ThrowingRunnable condition) {
        await(30, TimeUnit.SECONDS, condition);
    }

    public void awaitStatus(
        String clusterId,
        String deploymentId,
        Consumer<ConnectorDeploymentStatus> predicate) {

        awaitStatus(30, TimeUnit.SECONDS, clusterId, deploymentId, predicate);
    }

    public void awaitStatus(long timeout, TimeUnit unit, String clusterId, String deploymentId,
        Consumer<ConnectorDeploymentStatus> predicate) {
        await(
            timeout,
            unit,
            () -> {
                ConnectorDeploymentStatus status = getConnectorDeploymentStatus(clusterId, deploymentId);
                Assertions.assertThat(status)
                    .isNotNull()
                    .satisfies(predicate);
            });
    }

    protected Optional<ManagedConnector> getManagedConnector(ConnectorDeployment cd) {
        List<ManagedConnector> connectors = ksrv.getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, cd.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, cd.getId())
            .list()
            .getItems();

        if (connectors.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(connectors.get(0));
    }

    protected ManagedConnector mandatoryGetManagedConnector(ConnectorDeployment cd) {
        return getManagedConnector(cd)
            .orElseThrow(() -> new IllegalArgumentException("Unable to find a connector for deployment " + cd.getId()));
    }

    protected ManagedConnectorOperator withConnectorOperator(String name, String type, String version, String connectorsMeta) {
        return ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .createOrReplace(
                newConnectorOperator(namespace, name, type, version, connectorsMeta));
    }

    protected ConnectorDeploymentStatus getDeploymentStatus(ConnectorDeployment cd) {
        return fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .getConnector(cd.getId())
            .getStatus();
    }

    protected FleetManagerMock.ConnectorCluster getCluster() {
        return fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""));
    }

    public FleetManagerMock.Connector updateConnector(String clusterId, String deploymentId,
        Consumer<FleetManagerMock.Connector> consumer) {
        return fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .updateConnector(deploymentId, consumer);
    }

    public ConnectorDeploymentStatus getConnectorDeploymentStatus(String clusterId, String deploymentId) {
        return fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .getConnector(deploymentId)
            .getStatus();
    }
}
