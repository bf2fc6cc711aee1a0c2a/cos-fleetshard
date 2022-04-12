package org.bf2.cos.fleetshard.support;

public interface Service extends AutoCloseable {
    void start() throws Exception;

    void stop() throws Exception;

    default void close() throws Exception {
        stop();
    }
}
