package org.bf2.cos.fleetshard.operator.it.camel;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:CamelConnectorReifyWithTypeTrait.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.camel.glues"
    })
@TestProfile(CamelConnectorReifyWithTypeTraitTest.Profile.class)
public class CamelConnectorReifyWithTypeTraitTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-camel-" + uid();

            final Map<String, String> configs = new HashMap<>();

            configs.put("cos.namespace", ns);

            configs.put(
                "cos.operator.camel.connectors.traits.\"trait.camel.apache.org/container.image\"",
                "quay.io/foo/bar:latest");
            configs.put(
                "cos.operator.camel.connectors.types.\"log_sink_0.1\".traits.\"trait.camel.apache.org/container.image\"",
                "quay.io/bar/baz:latest");
            configs.put(
                "cos.operator.camel.connectors.traits.\"trait.camel.apache.org/kamelets.enabled\"",
                "true");
            configs.put(
                "cos.operator.camel.connectors.traits.\"trait.camel.apache.org/affinity.enabled\"",
                "true");

            return configs;
        }
    }
}
