package org.bf2.cos.fleetshard.support.watch;

import java.util.function.Consumer;

import org.bf2.cos.fleetshard.api.ManagedConnector;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

public final class Informers {
    private Informers() {
    }

    public static ResourceEventHandler<ManagedConnector> wrap(Consumer<ManagedConnector> delegate) {
        return new ResourceEventHandler<>() {
            @Override
            public void onAdd(ManagedConnector connector) {
                delegate.accept(connector);
            }

            @Override
            public void onUpdate(ManagedConnector oldConnector, ManagedConnector newConnector) {
                delegate.accept(newConnector);
            }

            @Override
            public void onDelete(ManagedConnector connector, boolean deletedFinalStateUnknown) {
                delegate.accept(connector);
            }
        };
    }
}
