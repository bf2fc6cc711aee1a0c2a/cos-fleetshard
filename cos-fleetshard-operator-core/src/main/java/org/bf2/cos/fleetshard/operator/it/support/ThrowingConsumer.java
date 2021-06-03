package org.bf2.cos.fleetshard.operator.it.support;

/**
 * Represents an operation that accepts a single input argument and may thrown an exception.
 *
 * @param <I> the type of the input to the operation
 * @param <T> the type of the exception the accept method may throw
 *
 * @see       java.util.function.Consumer
 */
@FunctionalInterface
public interface ThrowingConsumer<I, T extends Throwable> {
    /**
     * Performs this operation on the given argument, potentially throwing an exception.
     *
     * @param  in the function argument
     * @throws T  the exception that may be thrown
     */
    void accept(I in) throws T;
}