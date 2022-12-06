package org.bf2.cos.fleetshard.operator.it.camel;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:CamelConnectorReifyWithProcessors.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.camel.glues"
    })
@TestProfile(CamelConnectorReifyWithProcessorsTest.Profile.class)
public class CamelConnectorReifyWithProcessorsTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-camel-" + uid();

            return Map.of("cos.namespace", ns,
                "cos.operator.camel.connectors.processors.enabled", "true");
        }
    }
}
