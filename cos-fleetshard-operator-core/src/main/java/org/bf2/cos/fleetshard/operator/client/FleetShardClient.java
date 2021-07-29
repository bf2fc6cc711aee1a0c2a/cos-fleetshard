package org.bf2.cos.fleetshard.operator.client;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FleetShardClient {

    private final KubernetesClient kubernetesClient;
    private final String connectorsNamespace;
    private final String clusterNamespace;

    public FleetShardClient(
        KubernetesClient kubernetesClient,
        @ConfigProperty(name = "cos.cluster.namespace") String clusterNamespace,
        @ConfigProperty(name = "cos.connectors.namespace") String connectorsNamespace) {

        this.kubernetesClient = kubernetesClient;
        this.clusterNamespace = clusterNamespace;
        this.connectorsNamespace = connectorsNamespace;
    }

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getClusterNamespace() {
        return clusterNamespace;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public List<Operator> lookupOperators() {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(this.clusterNamespace)
            .list()
            .getItems()
            .stream()
            .map(mco -> new Operator(
                mco.getMetadata().getName(),
                mco.getSpec().getType(),
                mco.getSpec().getVersion(),
                mco.getSpec().getMetaService()))
            .collect(Collectors.toList());
    }

    public List<ManagedConnector> lookupManagedConnectors() {
        List<ManagedConnector> answer = kubernetesClient
            .customResources(ManagedConnector.class)
            .inNamespace(this.connectorsNamespace)
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public ManagedConnector create(ManagedConnector connector) {
        return kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .createOrReplace(connector);
    }

    public ManagedConnector edit(String name, Consumer<ManagedConnector> consumer) {
        return kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(connectorsNamespace)
            .withName(name).accept(consumer);
    }

    public Secret create(Secret secret) {
        return kubernetesClient.secrets()
            .inNamespace(connectorsNamespace)
            .createOrReplace(secret);
    }

    public ManagedConnectorOperator create(ManagedConnectorOperator connectorOperator) {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(clusterNamespace)
            .createOrReplace(connectorOperator);
    }
}
