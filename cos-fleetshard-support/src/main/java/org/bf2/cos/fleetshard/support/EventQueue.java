package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventQueue<T extends Comparable<T>, R> {
    private final Logger logger;
    private final ReentrantLock lock;
    private final TreeSet<Event<T>> events;

    public EventQueue() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.lock = new ReentrantLock();
        this.events = new TreeSet<>();
    }

    public int size() {
        this.lock.lock();

        try {
            return this.events.size();
        } finally {
            this.lock.unlock();
        }
    }

    public void poison() {
        this.lock.lock();

        try {
            this.events.add(new PoisonEvent<>());
        } finally {
            this.lock.unlock();
        }
    }

    public void submit(T element) {
        this.lock.lock();

        try {
            this.events.add(new SimpleEvent<T>(element));
        } finally {
            this.lock.unlock();
        }
    }

    public Collection<R> poll() {
        this.lock.lock();

        try {
            final Event<T> event = events.pollFirst();
            if (event == null) {
                return Collections.emptyList();
            }

            Collection<R> answer;

            if (event instanceof EventQueue.PoisonEvent) {
                answer = collectAll();
            } else {
                answer = collectAll(
                    Stream.concat(Stream.of(event), this.events.stream())
                        .map(Event::get)
                        .collect(Collectors.toList()));
            }

            events.clear();

            logger.debug("TaskQueue: answer={}, connectors={}", answer, answer.size());

            return answer;
        } finally {
            this.lock.unlock();
        }
    }

    protected abstract Collection<R> collectAll();

    protected abstract Collection<R> collectAll(Collection<T> elements);

    // *******************************
    //
    // Tasks
    //
    // *******************************

    public interface Event<T extends Comparable<T>> extends Comparable<Event<T>>, Supplier<T> {
    }

    public static class SimpleEvent<T extends Comparable<T>> implements Event<T> {
        private final T event;

        public SimpleEvent(T event) {
            this.event = event;
        }

        @Override
        public T get() {
            return this.event;
        }

        @Override
        public int compareTo(Event<T> o) {
            if (o instanceof EventQueue.PoisonEvent) {
                return 1;
            }

            return this.event.compareTo(o.get());
        }
    }

    public static class PoisonEvent<T extends Comparable<T>> implements Event<T> {

        @Override
        public T get() {
            return null;
        }

        @Override
        public int compareTo(Event<T> o) {
            if (o instanceof EventQueue.PoisonEvent) {
                return 0;
            }

            return -1;
        }
    }
}
