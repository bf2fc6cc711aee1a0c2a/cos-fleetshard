package org.bf2.cos.fleetshard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class FleetShardOperator {
    @Inject
    ManagedConnectorOperator managedConnectorOperator;
    @Inject
    KubernetesClient client;
    @Inject
    Operator operator;

    @ConfigProperty(name = "cos.operator.namespace")
    String operatorNamespace;

    void onStart(@Observes StartupEvent ignored) {
        client.resources(ManagedConnectorOperator.class)
            .inNamespace(operatorNamespace)
            .createOrReplace(managedConnectorOperator);

        operator.start();
    }

    void onStop(@Observes ShutdownEvent ignored) {
        operator.close();

        client.resources(ManagedConnectorOperator.class)
            .inNamespace(operatorNamespace)
            .withName(managedConnectorOperator.getMetadata().getName())
            .delete();
    }
}
