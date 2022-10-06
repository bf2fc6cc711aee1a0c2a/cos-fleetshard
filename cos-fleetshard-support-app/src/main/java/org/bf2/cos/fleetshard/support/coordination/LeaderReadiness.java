package org.bf2.cos.fleetshard.support.coordination;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.fabric8.kubernetes.client.BaseKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaderElectionRecord;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;

@Readiness
@ApplicationScoped
public class LeaderReadiness implements HealthCheck {
    private static final String CHECK_NAME = "leader_election";

    @Inject
    LeaseConfig config;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    LeaseLock lock;

    @Override
    public HealthCheckResponse call() {
        if (!config.enabled()) {
            return HealthCheckResponse.up(CHECK_NAME);
        }

        final LeaderElectionRecord rec = lock.get((BaseKubernetesClient) kubernetesClient);
        if (rec == null) {
            return HealthCheckResponse.up(CHECK_NAME);
        }

        final String identity = rec.getHolderIdentity();
        if (identity == null) {
            return HealthCheckResponse.up(CHECK_NAME);
        }

        var builder = HealthCheckResponse.builder()
            .name(CHECK_NAME)
            .withData("id", config.id())
            .withData("leader_id", identity);

        return Objects.equals(identity, config.id())
            ? builder.up().build()
            : builder.down().build();
    }
}