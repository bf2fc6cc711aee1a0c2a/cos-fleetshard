package org.bf2.cos.fleetshard.it.cucumber.assertions;

import java.util.Map;
import java.util.function.Function;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Strings;

import io.cucumber.datatable.DataTable;

import static org.assertj.core.api.Assertions.assertThat;

public class CucumberDataTableAssert extends AbstractAssert<CucumberDataTableAssert, DataTable> {
    private final Function<String, String> resolver;

    public CucumberDataTableAssert(DataTable actual, Function<String, String> resolver) {
        super(actual, CucumberDataTableAssert.class);

        this.resolver = resolver;
    }

    public <T> CucumberDataTableAssert matches(Map<String, T> elements) {
        isNotNull();

        Map<String, String> map = actual.asMap(String.class, String.class);
        map.forEach((k, v) -> {
            v = resolver.apply(v);

            if (Strings.isNullOrEmpty(v) || "${cos.ignore}".equals(v)) {
                assertThat(elements)
                    .describedAs("The key %s exists with any value", k)
                    .containsKey(k);
            } else {
                assertThat(elements.get(k))
                    .describedAs("The key %s exists with value %s", k, v)
                    .isNotNull()
                    .hasToString(v);
            }
        });

        return this;
    }
}
