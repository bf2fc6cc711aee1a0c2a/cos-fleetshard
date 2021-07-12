package org.bf2.cos.fleetshard.support;

@FunctionalInterface
public interface ThrowingRunnable<T extends Throwable> {
    void run() throws T;
}