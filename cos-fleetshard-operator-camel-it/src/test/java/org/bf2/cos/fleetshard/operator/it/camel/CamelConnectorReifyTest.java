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
        "classpath:CamelConnectorReify.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.camel.glues"
    })
@TestProfile(CamelConnectorReifyTest.Profile.class)
public class CamelConnectorReifyTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-camel-" + uid();

            final Map<String, String> configs = new HashMap<>();

            configs.put("cos.connectors.namespace", ns);
            configs.put("cos.operators.namespace", ns);
            configs.put("cos.operator.camel.route-controller.backoff-delay", "2s");
            configs.put("cos.operator.camel.route-controller.initial-delay", "1s");
            configs.put("cos.operator.camel.route-controller.backoff-multiplier", "2");
            configs.put("cos.operator.camel.health.readiness-success-threshold", "1");
            configs.put("cos.operator.camel.health.readiness-failure-threshold", "2");
            configs.put("cos.operator.camel.health.readiness-period-seconds", "3");
            configs.put("cos.operator.camel.health.readiness-timeout-seconds", "4");
            configs.put("cos.operator.camel.health.liveness-success-threshold", "5");
            configs.put("cos.operator.camel.health.liveness-failure-threshold", "6");
            configs.put("cos.operator.camel.health.liveness-period-seconds", "7");
            configs.put("cos.operator.camel.health.liveness-timeout-seconds", "8");
            return configs;
        }
    }
}
