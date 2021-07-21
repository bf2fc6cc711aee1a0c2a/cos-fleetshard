package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
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

    public void submitPoisonPill() {
        this.lock.lock();

        try {
            this.events.add(new Event<T>(null));
        } finally {
            this.lock.unlock();
        }
    }

    public void submit(T element) {
        Objects.requireNonNull(element, "Element must not be null");

        this.lock.lock();

        try {
            this.events.add(new Event<T>(element));
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

            if (event.event == null) {
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

    /**
     * Helper class to encapsulate an event where if the payload is null, then the event
     * has to be handled as a poison pill event.
     */
    private static class Event<T extends Comparable<T>> implements Comparable<Event<T>>, Supplier<T> {
        private final T event;

        public Event(T event) {
            this.event = event;
        }

        @Override
        public T get() {
            return this.event;
        }

        @Override
        public int compareTo(Event<T> o) {
            if (this.event == null) {
                return o.event != null ? -1 : 0;
            } else if (o.event == null) {
                return 1;
            }
            return this.event.compareTo(o.event);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Event)) {
                return false;
            }
            Event<?> event1 = (Event<?>) o;
            return Objects.equals(event, event1.event);
        }

        @Override
        public int hashCode() {
            return Objects.hash(event);
        }
    }

}
