package org.bf2.cos.fleetshard.support.function;

import java.util.function.Consumer;

public final class Functions {
    private Functions() {
    }

    public static <T> Consumer<T> noOpConsumer() {
        return item -> {
        };
    }
}
