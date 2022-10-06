package org.bf2.cos.fleetshard.support.coordination;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;

public class LeaseProducers {

    @Singleton
    @Produces
    public LeaseLock leaseLock(LeaseConfig config) {
        return new LeaseLock(config.namespace(), config.name(), config.id());
    }
}
