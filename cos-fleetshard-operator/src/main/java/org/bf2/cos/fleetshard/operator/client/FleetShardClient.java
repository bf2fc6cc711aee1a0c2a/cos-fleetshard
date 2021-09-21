package org.bf2.cos.fleetshard.operator.client;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class FleetShardClient {

    private final KubernetesClient kubernetesClient;
    private final String connectorsNamespace;
    private final String operatorNamespace;

    public FleetShardClient(
        KubernetesClient kubernetesClient,
        @ConfigProperty(name = "cos.operators.namespace") String operatorNamespace,
        @ConfigProperty(name = "cos.connectors.namespace") String connectorsNamespace) {

        this.kubernetesClient = kubernetesClient;
        this.operatorNamespace = operatorNamespace;
        this.connectorsNamespace = connectorsNamespace;
    }

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getOperatorNamespace() {
        return operatorNamespace;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public List<Operator> lookupOperators() {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(this.operatorNamespace)
            .list()
            .getItems()
            .stream()
            .map(mco -> new Operator(
                mco.getMetadata().getName(),
                mco.getSpec().getType(),
                mco.getSpec().getVersion()))
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
}
