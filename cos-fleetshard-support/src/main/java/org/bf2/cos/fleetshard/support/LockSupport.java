package org.bf2.cos.fleetshard.support;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import org.bf2.cos.fleetshard.support.function.ThrowingRunnable;

public final class LockSupport {
    private LockSupport() {
    }

    public static void doWithLock(Lock lock, Runnable action) {
        lock.lock();

        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public static <T extends Throwable> void doWithLockT(Lock lock, ThrowingRunnable<T> action) throws T {
        lock.lock();

        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public static <T> T callWithLock(Lock lock, Supplier<T> action) {
        lock.lock();

        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
