package org.bf2.cos.fleetshard.operator.it.debezium;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:DebeziumConnectorReifyWithExternalConfig.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.debezium.glues"
    })
@TestProfile(DebeziumConnectorReifyWithExternalConfigTest.Profile.class)
public class DebeziumConnectorReifyWithExternalConfigTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-debezium-" + uid();

            return Map.of(
                "cos.connectors.namespace", ns,
                "cos.operators.namespace", ns,
                "cos.operator.debezium.kafka-connect.config.\"config.storage.replication.factor\"", "3");
        }
    }
}
