package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventQueue<T extends Comparable<T>, R> {
    private final Logger logger;
    private final ReentrantLock lock;
    private final Condition condition;
    private final Set<T> events;
    private volatile boolean poison;

    public EventQueue() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.poison = false;
        this.events = new HashSet<>();
    }

    public int size() {
        return LockSupport.callWithLock(lock, this.events::size);
    }

    public boolean isPoisoned() {
        return poison;
    }

    public void submitPoisonPill() {
        LockSupport.doWithLock(this.lock, () -> {
            this.poison = true;
            this.condition.signalAll();
        });
    }

    public void submit(T element) {
        Objects.requireNonNull(element, "Element must not be null");

        LockSupport.doWithLock(this.lock, () -> {
            this.events.add(element);
            this.condition.signalAll();
        });
    }

    public void run(Consumer<Collection<R>> consumer) {
        LockSupport.doWithLock(this.lock, () -> {
            if (!poison && events.isEmpty()) {
                return;
            }

            process(poison ? Collections.emptyList() : events, consumer);

            poison = false;
            events.clear();
        });
    }

    public void poll(long time, TimeUnit unit, Consumer<Collection<R>> consumer) throws InterruptedException {
        LockSupport.doWithLockT(this.lock, () -> {
            if (events.isEmpty() && !poison) {
                boolean elapsed = this.condition.await(time, unit);
                if (elapsed) {
                    logger.trace("TaskQueue: await elapsed");
                }
            }

            process(poison ? Collections.emptyList() : events, consumer);

            poison = false;
            events.clear();
        });
    }

    protected abstract void process(Collection<T> elements, Consumer<Collection<R>> consumer);
}
