package org.bf2.cos.fleetshard.support.watch;

import java.util.function.Consumer;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

public final class Informers {
    private Informers() {
    }

    public static <T> ResourceEventHandler<T> wrap(Consumer<T> delegate) {
        return new ResourceEventHandler<>() {
            @Override
            public void onAdd(T resource) {
                delegate.accept(resource);
            }

            @Override
            public void onUpdate(T oldResource, T newResource) {
                delegate.accept(newResource);
            }

            @Override
            public void onDelete(T resource, boolean deletedFinalStateUnknown) {
                delegate.accept(resource);
            }
        };
    }
}
