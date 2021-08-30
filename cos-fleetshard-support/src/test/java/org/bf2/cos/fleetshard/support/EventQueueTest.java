package org.bf2.cos.fleetshard.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueueTest {
    private EventQueue<String, String> queue;

    @BeforeEach
    private void setUp() {
        this.queue = new EventQueue<>() {
            @Override
            protected Collection<String> collectAll() {
                return Collections.singleton("x");
            }

            @Override
            protected Collection<String> collectAll(Collection<String> elements) {
                return elements.stream()
                    .map(e -> e.toLowerCase(Locale.US))
                    .collect(Collectors.toList());
            }
        };
    }

    @Test
    void contains() throws InterruptedException {
        queue.submit("A");
        queue.submit("B");
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.poll(1, TimeUnit.MILLISECONDS)).containsOnly("a", "b", "c");
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void poison() throws InterruptedException {
        queue.submit("A");
        queue.submit("B");
        queue.submitPoisonPill();
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.isPoisoned()).isTrue();
        assertThat(queue.poll(1, TimeUnit.MILLISECONDS)).containsOnly("x");
        assertThat(queue.size()).isEqualTo(0);
        assertThat(queue.isPoisoned()).isFalse();
    }

    @Test
    void poison2() throws InterruptedException {
        queue.submit("A");
        queue.submitPoisonPill();
        queue.submit("B");
        queue.submitPoisonPill();
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.isPoisoned()).isTrue();
        assertThat(queue.poll(1, TimeUnit.MILLISECONDS)).containsOnly("x");
        assertThat(queue.size()).isEqualTo(0);
        assertThat(queue.isPoisoned()).isFalse();
    }
}
