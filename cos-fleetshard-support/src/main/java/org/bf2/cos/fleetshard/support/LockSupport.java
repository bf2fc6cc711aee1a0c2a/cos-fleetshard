package org.bf2.cos.fleetshard.support;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.bf2.cos.fleetshard.support.function.ThrowingRunnable;
import org.bf2.cos.fleetshard.support.function.ThrowingSupplier;

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

    public static <R> R callWithReadLock(StampedLock lock, Callable<R> task) throws Exception {
        long stamp = lock.readLock();

        try {
            return task.call();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public static <R, T extends Throwable> R supplyWithReadLockT(StampedLock lock, ThrowingSupplier<R, T> task) throws T {
        long stamp = lock.readLock();

        try {
            return task.get();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public static void doWithReadLock(StampedLock lock, Runnable task) {
        long stamp = lock.readLock();

        try {
            task.run();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public static <T extends Throwable> void doWithReadLockT(StampedLock lock, ThrowingRunnable<T> task) throws T {
        long stamp = lock.readLock();

        try {
            task.run();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public static void doWithWriteLock(StampedLock lock, Runnable task) {
        long stamp = lock.writeLock();

        try {
            task.run();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public static <T extends Throwable> void doWithWriteLockT(StampedLock lock, ThrowingRunnable<T> task) throws T {
        long stamp = lock.writeLock();

        try {
            task.run();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public static <R> R callWithWriteLock(StampedLock lock, Callable<R> task) throws Exception {
        long stamp = lock.writeLock();

        try {
            return task.call();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public static <R, T extends Throwable> R supplyWithWriteLockT(StampedLock lock, ThrowingSupplier<R, T> task) throws T {
        long stamp = lock.writeLock();

        try {
            return task.get();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
