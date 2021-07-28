package org.bf2.cos.fleetshard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.bf2.cos.fleetshard.operator.client.FleetShardClient;

@ApplicationScoped
public class FleetShardOperator {
    @Inject
    FleetShardClient fleetShardClient;
    @Inject
    Operator operator;

    void onStart(@Observes StartupEvent ignored) {
        fleetShardClient.lookupOrCreateManagedConnectorCluster();

        operator.start();
    }

    void onStop(@Observes ShutdownEvent ignored) {
        operator.close();
    }
}
