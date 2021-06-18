package org.bf2.cos.fleetshard.operator.it.support;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeployment;
import org.bf2.cos.fleet.manager.api.model.cp.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;

public class CamelTestSupport {
    static {
        KubernetesDeserializer.registerCustomKind("camel.apache.org/v1alpha1", "KameletBinding", KameletBindingResource.class);
    }

    @KubernetesTestServer
    protected KubernetesServer ksrv;

    @Inject
    protected FleetManager fm;

    @ConfigProperty(
        name = "cluster-id")
    protected String clusterId;

    @ConfigProperty(
        name = "cos.connectors.namespace")
    protected String connectorsNamespace;

    @ConfigProperty(
        name = "cos.fleetshard.meta.camel")
    protected String connectorsMeta;

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

    @JsonDeserialize
    public static class KameletBindingResource extends HashMap<String, Object> implements KubernetesResource {
    }

    protected List<ManagedConnector> getManagedConnectors(ConnectorDeployment cd) {
        return ksrv.getClient()
            .customResources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withLabel(ManagedConnector.LABEL_CONNECTOR_ID, cd.getSpec().getConnectorId())
            .withLabel(ManagedConnector.LABEL_DEPLOYMENT_ID, cd.getId())
            .list()
            .getItems();
    }

    protected ManagedConnectorOperator withConnectorOperator(String name, String version) {
        return ksrv.getClient()
            .customResources(ManagedConnectorOperator.class)
            .inNamespace(connectorsNamespace)
            .createOrReplace(
                newConnectorOperator(connectorsNamespace, name, version, connectorsMeta));
    }

    protected ConnectorDeploymentStatus getDeploymentStatus(ConnectorDeployment cd) {
        return fm.getCluster(clusterId)
            .orElseThrow(() -> new IllegalStateException(""))
            .getConnector(cd.getId())
            .getStatus();
    }
}
