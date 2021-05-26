package org.bf2.cos.fleetshard.operator.support;

/**
 * Represents a function that accepts a single arguments, produces a result and may thrown an exception.
 *
 * @param <I> the type of the input of the function
 * @param <R> the type of the result of the function
 * @param <T> the type of the exception the accept method may throw
 *
 * @see       java.util.function.Function
 */
@FunctionalInterface
public interface ThrowingFunction<I, R, T extends Throwable> {
    /**
     * Applies this function to the given argument, potentially throwing an exception.
     *
     * @param  in the function argument
     * @return    the function result
     * @throws T  the exception that may be thrown
     */
    R apply(I in) throws T;
}