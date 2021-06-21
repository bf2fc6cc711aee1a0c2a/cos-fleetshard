package org.bf2.cos.fleetshard.operator.support;

@FunctionalInterface
public interface ThrowingRunnable<T extends Throwable> {
    void run() throws T;
}