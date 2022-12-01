package org.bf2.cos.fleetshard.it.cucumber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;

import static org.assertj.core.api.Assertions.assertThat;

public class MicrometerSteps extends StepsSupport {

    @Inject
    MeterRegistry registry;

    private final Map<String, Double> counters = new HashMap<>();

    @Before
    public void setUp() {
        registry.clear();
        counters.clear();
    }

    // ***********************************
    //
    // Counters
    //
    // ***********************************

    @And("the meters has counter with name {string}")
    public void counter_exists(String name) {
        assertThat(registry.find(name).counter())
            .isNotNull();
    }

    @And("the meters does not have any counter with name {string}")
    public void counter_doesNotExists(String name) {
        assertThat(registry.find(name).counter())
            .isNull();
    }

    @And("the meters has counter {string} with value greater than {int}")
    public void counter_isGreaterThan(String name, int expected) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isGreaterThan(expected));
    }

    @And("the meters has counter {string} with value greater than or equal to {int}")
    public void counter_isGreaterThanOrEqualTo(String name, int expected) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isGreaterThanOrEqualTo(expected));
    }

    @And("the meters has counter {string} with value equal to {int}")
    public void counter_isEqualTo(String name, int expected) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isEqualTo(expected));
    }

    @Then("wait till meters has counter {string} with value equal to {int}")
    public void until_counter_isEqualTo(String name, int expected) {
        awaiter.untilAsserted(() -> {
            counter_isEqualTo(name, expected);
        });
    }

    @And("the meters has counter {string} with value less than or equal to {int}")
    public void counter_isLessThanOrEqualTo(String name, int expected) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isLessThanOrEqualTo(expected));
    }

    @And("the meters has counter {string} with value less than {int}")
    public void counter_isLessThan(String name, int expected) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isLessThan(expected));
    }

    @Then("save the meters value of counter {string}")
    public void counter_storeValue(String name) {
        Counter counter = registry.find(name).counter();
        if (counter != null) {
            counters.put(name, counter.count());
        }
    }

    @Then("the meters value of counter {string} has not changed")
    public void counter_valueHasNotChanged(String name) {
        assertThat(registry.find(name).counter())
            .isNotNull()
            .satisfies(counter -> assertThat(counter.count()).isEqualTo(counters.get(name)));
    }

    @And("the meters has counter with name {string} and tag {string}")
    public void counter_existsWithTag(String name, String tagName) {
        assertThat(registry.find(name).tagKeys(tagName).counter())
            .isNotNull();
    }

    @And("the meters has counter with name {string} and tags:")
    public void counter_existsWithTags(String name, Map<String, String> entry) {
        var tags = entry.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), ctx.resolvePlaceholders(e.getValue())))
            .collect(Collectors.toList());

        assertThat(registry.find(name).tags(tags).counter())
            .withFailMessage(() -> String.format("Counter with id '%s' and tags '%s' not found", name, entry))
            .isNotNull();
    }

    @And("wait till the meters has counter {string} with value equal to {int}")
    public void counter_wait_isEqualTo(String name, int expected) {
        awaiter.until(() -> {
            Counter counter = registry.find(name).counter();
            if (counter == null) {
                return false;
            }

            return ((int) counter.count()) == expected;
        });
    }

    @And("wait till the meters has counter {string} with value greater than or equal to {int}")
    public void counter_wait_isGreaterThanOrEqualTo(String name, int expected) {
        awaiter.until(() -> {
            Counter counter = registry.find(name).counter();
            if (counter == null) {
                return false;
            }

            return ((int) counter.count()) >= expected;
        });
    }

    @And("the meters has entry with name {string} and tags:")
    public void meter_exists_with_tags(String name, Map<String, String> entry) {
        Search s = registry.find(name);

        assertThat(s).isNotNull();
        assertThat(s.meters()).isNotEmpty();

        assertThat(s.meters()).anySatisfy(m -> {
            for (var expectedTag : entry.entrySet()) {
                final String tagName = ctx.resolvePlaceholders(expectedTag.getKey());
                final String expected = ctx.resolvePlaceholders(expectedTag.getValue());
                final String current = m.getId().getTag(tagName);

                assertThat(current)
                    .withFailMessage(() -> String.format("Meter with id '%s' does not have a tag '%s' with value '%s'", name,
                        tagName, expected))
                    .isEqualTo(expected);
            }
        });
    }

    @And("the meters has entries with name matching {string} and tags:")
    public void meters_exists_with_tags(String regex, Map<String, String> entry) {
        List<Meter> meters = registry.getMeters();
        assertThat(meters).isNotEmpty();

        int matches = 0;

        for (Meter meter : meters) {
            if (!meter.getId().getName().matches(regex)) {
                continue;
            }

            matches++;

            assertThat(meter).satisfies(m -> {
                for (var expectedTag : entry.entrySet()) {
                    final String tagName = ctx.resolvePlaceholders(expectedTag.getKey());
                    final String expected = ctx.resolvePlaceholders(expectedTag.getValue());
                    final String current = m.getId().getTag(tagName);

                    assertThat(current)
                        .withFailMessage(() -> String.format("Meter with name '%s' does not have a tag '%s' with value '%s'",
                            meter.getId().getName(),
                            tagName,
                            expected))
                        .isEqualTo(expected);
                }
            });
        }

        assertThat(matches)
            .withFailMessage(() -> String.format("No meters matching '%s'", regex))
            .isGreaterThan(0);
    }

    // ***********************************
    //
    // timers
    //
    // ***********************************

    @And("the meters has timer with name {string}")
    public void timer_exists(String name) {
        assertThat(registry.find(name).timer())
            .isNotNull();
    }

    @And("the meters has timer with name {string} and tag {string}")
    public void timer_existsWithTag(String name, String tagName) {
        assertThat(registry.find(name).tagKeys(tagName).timer())
            .isNotNull();
    }

    @And("the meters has timer with name {string} and tags:")
    public void timer_existsWithTags(String name, Map<String, String> entry) {

        var tags = entry.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), ctx.resolvePlaceholders(e.getValue())))
            .collect(Collectors.toList());

        assertThat(registry.find(name).tags(tags).timer())
            .withFailMessage(() -> String.format("Timer with id '%s' and tags '%s' not found", name, entry))
            .isNotNull();
    }

    @And("the meters does not have any timer with name {string}")
    public void timer_doesNotExists(String name) {
        assertThat(registry.find(name).timer())
            .isNull();
    }

    @And("wait till the meters has timer with name {string}")
    public void timer_wait_exists(String name) {
        awaiter.until(() -> registry.find(name).timer() != null);
    }

}
