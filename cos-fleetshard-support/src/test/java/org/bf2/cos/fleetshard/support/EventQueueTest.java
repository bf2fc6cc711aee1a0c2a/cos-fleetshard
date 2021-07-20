package org.bf2.cos.fleetshard.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void contains() {
        queue.submit("A");
        queue.submit("B");
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.poll()).containsOnly("a", "b", "c");
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void poison() {
        queue.submit("A");
        queue.submit("B");
        queue.poison();
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(4);
        assertThat(queue.poll()).containsOnly("x");
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void poison2() {
        queue.submit("A");
        queue.poison();
        queue.submit("B");
        queue.poison();
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(4);
        assertThat(queue.poll()).containsOnly("x");
        assertThat(queue.size()).isEqualTo(0);
    }
}
