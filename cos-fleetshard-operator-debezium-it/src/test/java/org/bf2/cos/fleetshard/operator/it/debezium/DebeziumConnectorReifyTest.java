package org.bf2.cos.fleetshard.operator.it.debezium;

import java.util.Map;

import org.bf2.cos.fleetshard.it.resources.BaseTestProfile;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
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
    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            final String ns = "cos-" + uid();

            return Map.of(
                "test.namespace", ns,
                "cos.connectors.namespace", ns,
                "cos.operators.namespace", ns,
                "cos.cluster.id", uid());
        }
    }
}
