package org.bf2.cos.fleetshard.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
            protected void process(Collection<String> elements, Consumer<Collection<String>> consumer) {
                final Collection<String> items;

                if (elements.isEmpty()) {
                    items = Collections.singleton("x");
                } else {
                    items = elements.stream()
                        .map(e -> e.toLowerCase(Locale.US))
                        .collect(Collectors.toList());
                }

                consumer.accept(items);
            }
        };
    }

    @Test
    void contains() throws InterruptedException {
        queue.submit("A");
        queue.submit("B");
        queue.submit("C");

        assertThat(queue.size()).isEqualTo(3);

        List<String> answer = new ArrayList<>();
        queue.poll(1, TimeUnit.MILLISECONDS, answer::addAll);
        assertThat(answer).containsOnly("a", "b", "c");

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

        List<String> answer = new ArrayList<>();
        queue.poll(1, TimeUnit.MILLISECONDS, answer::addAll);
        assertThat(answer).containsOnly("x");

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

        List<String> answer = new ArrayList<>();
        queue.poll(1, TimeUnit.MILLISECONDS, answer::addAll);
        assertThat(answer).containsOnly("x");

        assertThat(queue.size()).isEqualTo(0);
        assertThat(queue.isPoisoned()).isFalse();
    }
}
