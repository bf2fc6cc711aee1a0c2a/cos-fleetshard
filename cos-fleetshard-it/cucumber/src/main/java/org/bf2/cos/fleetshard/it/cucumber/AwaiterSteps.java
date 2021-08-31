package org.bf2.cos.fleetshard.it.cucumber;

import java.util.Map;

import javax.inject.Inject;

import io.cucumber.java.en.Given;

public class AwaiterSteps {
    @Inject
    Awaiter awaiter;

    @Given("^Await configuration$")
    public void configure_await(Map<String, String> configuration) {
        if (configuration.containsKey("atMost")) {
            awaiter.atMost = Long.parseLong(configuration.get("atMost"));
        }
        if (configuration.containsKey("pollDelay")) {
            awaiter.pollDelay = Long.parseLong(configuration.get("pollDelay"));
        }
        if (configuration.containsKey("pollInterval")) {
            awaiter.pollInterval = Long.parseLong(configuration.get("pollInterval"));
        }
    }
}
