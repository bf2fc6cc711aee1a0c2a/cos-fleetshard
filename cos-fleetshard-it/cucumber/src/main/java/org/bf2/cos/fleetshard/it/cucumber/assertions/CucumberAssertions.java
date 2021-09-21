package org.bf2.cos.fleetshard.it.cucumber.assertions;

import java.util.function.Function;

import io.cucumber.datatable.DataTable;

public final class CucumberAssertions {
    private CucumberAssertions() {
    }

    public static CucumberDataTableAssert assertThatDataTable(DataTable actual) {
        return assertThatDataTable(actual, Function.identity());
    }

    public static CucumberDataTableAssert assertThatDataTable(DataTable actual, Function<String, String> resolver) {
        return new CucumberDataTableAssert(actual, resolver);
    }
}
