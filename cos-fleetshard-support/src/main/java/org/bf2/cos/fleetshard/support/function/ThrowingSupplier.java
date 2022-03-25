package org.bf2.cos.fleetshard.support.function;

@FunctionalInterface
public interface ThrowingSupplier<V, T extends Throwable> {
    V get() throws T;
}
