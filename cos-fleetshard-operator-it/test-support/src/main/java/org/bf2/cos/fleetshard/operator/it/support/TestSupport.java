package org.bf2.cos.fleetshard.operator.it.support;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.awaitility.Awaitility;
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
    String namespace;

    public static void await(long timeout, TimeUnit unit, Callable<Boolean> condition) {
        Awaitility.await()
            .atMost(timeout, unit)
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(condition);
    }

    public static ManagedConnectorOperator newConnectorOperator(
        String namespace,
        String name,
        String version,
        String connectorsMeta) {

        return new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withNamespace(namespace)
                .withName(name)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withType("camel-connector-operator")
                .withVersion(version)
                .withMetaService(connectorsMeta)
                .build())
            .build();
    }

    protected List<ManagedConnector> getManagedConnectors(ConnectorDeployment cd) {
        return ksrv.getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, cd.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, cd.getId())
            .list()
            .getItems();
    }

    protected ManagedConnectorOperator withConnectorOperator(String name, String version, String connectorsMeta) {
        return ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(namespace)
            .createOrReplace(
                TestSupport.newConnectorOperator(namespace, name, version, connectorsMeta));
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
}
