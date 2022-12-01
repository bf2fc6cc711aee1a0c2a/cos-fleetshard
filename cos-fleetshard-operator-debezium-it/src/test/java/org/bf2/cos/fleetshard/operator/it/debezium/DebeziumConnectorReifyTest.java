package org.bf2.cos.fleetshard.operator.it.debezium;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:DebeziumConnectorReify.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.debezium.glues"
    })
@TestProfile(DebeziumConnectorReifyTest.Profile.class)
public class DebeziumConnectorReifyTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-debezium-" + uid();
            final Map<String, String> configs = new HashMap<>();

            configs.put("cos.namespace", ns);
            configs.put("cos.metrics.connector-operand.enabled", "true");
            configs.put("cos.metrics.recorder.tags.common.foo", "bar");
            configs.put("cos.metrics.recorder.tags.annotations[0]", "my.cos.bf2.org/connector-group");
            configs.put("cos.metrics.recorder.tags.labels[0]", "cos.bf2.org/organisation-id");
            configs.put("cos.metrics.recorder.tags.labels[1]", "cos.bf2.org/pricing-tier");

            return configs;
        }
    }
}
