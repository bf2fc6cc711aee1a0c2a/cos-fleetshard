package org.bf2.cos.fleetshard.it.cucumber;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.micrometer.core.instrument.MeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;

public class MicrometerSteps extends StepsSupport {

    @Inject
    MeterRegistry registry;

    @Before
    public void setUp() {
        registry.clear();
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

    @And("the meters does not have any timer with name {string}")
    public void timer_doesNotExists(String name) {
        assertThat(registry.find(name).counter())
            .isNull();
    }

}
