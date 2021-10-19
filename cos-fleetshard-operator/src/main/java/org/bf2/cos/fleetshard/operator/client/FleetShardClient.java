package org.bf2.cos.fleetshard.operator.client;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.Operator;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class FleetShardClient {

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    FleetShardOperatorConfig config;

    public String getConnectorsNamespace() {
        return config.connectors().namespace();
    }

    public String getOperatorNamespace() {
        return config.operators().namespace();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public List<Operator> lookupOperators() {
        return kubernetesClient.customResources(ManagedConnectorOperator.class)
            .inNamespace(getOperatorNamespace())
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
            .inNamespace(getConnectorsNamespace())
            .list()
            .getItems();

        return answer != null ? answer : Collections.emptyList();
    }

    public ManagedConnector create(ManagedConnector connector) {
        return kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .createOrReplace(connector);
    }

    public ManagedConnector edit(String name, Consumer<ManagedConnector> consumer) {
        return kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(getConnectorsNamespace())
            .withName(name).accept(consumer);
    }

    public Secret create(Secret secret) {
        return kubernetesClient.secrets()
            .inNamespace(getConnectorsNamespace())
            .createOrReplace(secret);
    }
}
