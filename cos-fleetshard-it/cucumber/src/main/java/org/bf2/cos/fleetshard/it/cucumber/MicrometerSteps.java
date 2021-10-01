package org.bf2.cos.fleetshard.it.cucumber;

import javax.inject.Inject;

import io.cucumber.java.en.And;
import io.micrometer.core.instrument.MeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;

public class MicrometerSteps {

    @Inject
    MeterRegistry registry;

    // ***********************************
    //
    // Counters
    //
    // ***********************************

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

    // ***********************************
    //
    // Counters
    //
    // ***********************************

    @And("the meters has timer {string}")
    public void timer_exists(String name) {
        assertThat(registry.find(name).timer())
            .isNotNull();
    }

}
